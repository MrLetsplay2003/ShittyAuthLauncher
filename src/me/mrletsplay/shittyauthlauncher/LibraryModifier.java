package me.mrletsplay.shittyauthlauncher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import me.mrletsplay.mrcore.io.IOUtils;
import me.mrletsplay.mrcore.io.ZIPFileUtils;
import me.mrletsplay.mrcore.io.ZipOut;
import me.mrletsplay.mrcore.misc.classfile.ByteCode;
import me.mrletsplay.mrcore.misc.classfile.ClassField;
import me.mrletsplay.mrcore.misc.classfile.ClassFile;
import me.mrletsplay.mrcore.misc.classfile.ClassMethod;
import me.mrletsplay.mrcore.misc.classfile.Instruction;
import me.mrletsplay.mrcore.misc.classfile.InstructionInformation;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolFieldRefEntry;
import me.mrletsplay.mrcore.misc.classfile.util.ClassFileUtils;
import me.mrletsplay.shittyauthlauncher.version.MinecraftVersion;

public class LibraryModifier {
	
	public static File patchAuthlib(File authLib, MinecraftVersion version) throws IOException {
		File out = new File(ShittyAuthLauncherSettings.getGameDataPath(), "libraries/authlib-" + version.getId() + ".jar");
		if(out.exists() && !ShittyAuthLauncherSettings.isAlwaysPatchAuthlib()) return out;
		
		File cringeFolder = new File(ShittyAuthLauncherSettings.getGameDataPath(), "cringe");
		cringeFolder.mkdirs();
		
		ZIPFileUtils.unzipFile(authLib, cringeFolder);

		File yggFile = new File(cringeFolder, "com/mojang/authlib/yggdrasil/YggdrasilMinecraftSessionService.class");
		ClassFile cf = new ClassFile(yggFile);
		
		ClassField domainsField = null;
		for(ClassField f : cf.getFields()) {
			if(f.getName().getValue().equals("WHITELISTED_DOMAINS")
					|| f.getName().getValue().equals("ALLOWED_DOMAINS")) domainsField = f;
		}
		
		if(domainsField != null) { // Otherwise the version is probably too old (e.g. 1.7.2), and doesn't have whitelisted URLs
			ClassMethod meth = cf.getMethods("<clinit>")[0];
			List<InstructionInformation> iis = meth.getCodeAttribute().getCode().parseCode();
			int startIdx = -1, endIdx = -1; // Find beginning and end of array initialization
			for(int i = 0; i < iis.size(); i++) {
				InstructionInformation ii = iis.get(i);
				if(ii.getInstruction() == Instruction.ANEWARRAY && startIdx == -1) {
					startIdx = i;
				}
				
				if(ii.getInstruction() == Instruction.PUTSTATIC) {
					short s = 0;
					s += (ii.getInformation()[0] & 0xFF) << 8;
					s += ii.getInformation()[1] & 0xFF;
					
					ConstantPoolFieldRefEntry fr = (ConstantPoolFieldRefEntry) cf.getConstantPool().getEntry(s);
					String fieldName = fr.getNameAndType().getName().getValue();
					if(fieldName.equals(domainsField.getName().getValue())) { // BLOCKED_DOMAINS / WHITELISTED_DOMAINS
						endIdx = i;
					}
				}
			}
			
			String host = ShittyAuthLauncherSettings.getSkinHost();
			System.out.println("Patching with host: " + host);
			int en = ClassFileUtils.getOrAppendString(cf, ClassFileUtils.getOrAppendUTF8(cf, host));
			int en2 = ClassFileUtils.getOrAppendString(cf, ClassFileUtils.getOrAppendUTF8(cf, ".minecraft.net"));
			
			iis.subList(0, endIdx).clear();
			
			List<InstructionInformation> newInstrs = new ArrayList<>();
			newInstrs.add(new InstructionInformation(Instruction.ICONST_2));
			newInstrs.add(new InstructionInformation(Instruction.ANEWARRAY, ClassFileUtils.getShortBytes(ClassFileUtils.getOrAppendClass(cf, ClassFileUtils.getOrAppendUTF8(cf, "java/lang/String")))));
			newInstrs.add(new InstructionInformation(Instruction.DUP));
			newInstrs.add(new InstructionInformation(Instruction.ICONST_0));
			newInstrs.add(new InstructionInformation(Instruction.LDC_W, ClassFileUtils.getShortBytes(en)));
			newInstrs.add(new InstructionInformation(Instruction.AASTORE));
			newInstrs.add(new InstructionInformation(Instruction.DUP));
			newInstrs.add(new InstructionInformation(Instruction.ICONST_1));
			newInstrs.add(new InstructionInformation(Instruction.LDC_W, ClassFileUtils.getShortBytes(en2)));
			newInstrs.add(new InstructionInformation(Instruction.AASTORE));
			iis.addAll(0, newInstrs);
			
			meth.getCodeAttribute().getCode().replace(ByteCode.of(iis));
			
			try(FileOutputStream fOut = new FileOutputStream(yggFile)) {
				cf.write(fOut);
			}
		}

		File pubkeyFile = new File(cringeFolder, "yggdrasil_session_pubkey.der");
		
		URL url = ShittyAuthLauncher.class.getResource("/include/yggdrasil_session_pubkey.der");
		if(url == null) url = new File("./include/yggdrasil_session_pubkey.der").toURI().toURL();
		try(InputStream in = url.openStream()) {
			IOUtils.writeBytes(pubkeyFile, IOUtils.readAllBytes(in));
		}
		
		System.out.println("Repacking...");
		ZipOut z = new ZipOut(out);
		for(File fl : cringeFolder.listFiles()) {
			z.writeFile(fl, f -> {
				System.out.println("Adding " + f.getPath());
			});
		}
		z.close();
		IOUtils.deleteFile(cringeFolder);
		System.out.println("Done!");
		return out;
	}

}
