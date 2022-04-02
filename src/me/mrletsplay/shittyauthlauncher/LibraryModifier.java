package me.mrletsplay.shittyauthlauncher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import me.mrletsplay.mrcore.misc.classfile.ByteCode;
import me.mrletsplay.mrcore.misc.classfile.ClassField;
import me.mrletsplay.mrcore.misc.classfile.ClassFile;
import me.mrletsplay.mrcore.misc.classfile.ClassMethod;
import me.mrletsplay.mrcore.misc.classfile.Instruction;
import me.mrletsplay.mrcore.misc.classfile.InstructionInformation;
import me.mrletsplay.mrcore.misc.classfile.attribute.AttributeCode;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolEntry;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolFieldRefEntry;
import me.mrletsplay.mrcore.misc.classfile.pool.entry.ConstantPoolStringEntry;
import me.mrletsplay.mrcore.misc.classfile.util.ClassFileUtils;
import me.mrletsplay.shittyauthlauncher.version.MinecraftVersion;

public class LibraryModifier {
	
	// TODO: patch for older versions (< MC 1.16):
	// - Replace string constant "https://sessionserver.mojang.com/session/minecraft/profile/"
	// - Replace static variables JOIN_URL, CHECK_URL
	
	public static File patchAuthlib(File authLib, MinecraftVersion version) throws IOException {
		File out = new File(ShittyAuthLauncherSettings.getGameDataPath(), "libraries/authlib-" + version.getId() + ".jar");
		if(out.exists() && !ShittyAuthLauncherSettings.isAlwaysPatchAuthlib()) return out;
		
		Files.copy(authLib.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
		
		try(FileSystem fs = FileSystems.newFileSystem(out.toPath(), (ClassLoader) null)) {
			Path sessionService = fs.getPath("com/mojang/authlib/yggdrasil/YggdrasilMinecraftSessionService.class");
			
			ClassFile cf;
			try(InputStream in = Files.newInputStream(sessionService)) {
				cf = new ClassFile(in);
			}
			
			ClassField domainsField = null;
			for(ClassField f : cf.getFields()) {
				if(f.getName().getValue().equals("WHITELISTED_DOMAINS")
						|| f.getName().getValue().equals("ALLOWED_DOMAINS")) domainsField = f;
			}
			
			if(domainsField != null) { // Otherwise the version is probably too old (e.g. 1.7.2), and doesn't have whitelisted URLs
				ClassMethod meth = cf.getMethods("<clinit>")[0];
				
				AttributeCode codeAttr = meth.getCodeAttribute();
				
				ByteCode code = codeAttr.getCode();
				List<InstructionInformation> iis = code.parseCode();
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
				
				code.replace(ByteCode.of(iis));
			}

			replaceSessionServer(cf);
			
			try(OutputStream fOut = Files.newOutputStream(sessionService)) {
				cf.write(fOut);
			}
			
			Path pubkeyPath = fs.getPath("yggdrasil_session_pubkey.der");
			File launcherPubkeyFile = new File("shittyauthlauncher/yggdrasil_session_pubkey.der");
			if(launcherPubkeyFile.exists()) {
				Files.copy(launcherPubkeyFile.toPath(), pubkeyPath, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		
		System.out.println("Done!");
		return out;
	}
	
	private static void replaceSessionServer(ClassFile cf) {
		for(int i = 1; i < cf.getConstantPool().getSize() + 1; i++) {
			ConstantPoolEntry e = cf.getConstantPool().getEntry(i);
			if(e instanceof ConstantPoolStringEntry) {
				String s = e.as(ConstantPoolStringEntry.class).getString().getValue();
				if(s.startsWith("https://sessionserver.mojang.com")) {
					cf.getConstantPool().setEntry(i, new ConstantPoolStringEntry(cf.getConstantPool(), ClassFileUtils.getOrAppendUTF8(cf, s.replace("https://sessionserver.mojang.com", ShittyAuthLauncherSettings.getSessionServerURL()))));
				}
			}
		}
	}

}
