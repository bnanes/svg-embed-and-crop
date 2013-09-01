import subprocess, os.path, sys, platform
cp = os.path.expandvars("${APPDATA}/inkscape/extensions/svg-embed-and-crop/*")
if len(sys.argv) > 1:
	f = sys.argv[1]
else:
	f = ""
java = "java -cp \""
if(platform.system() == "Windows"):
	if os.path.exists("C:\\Program Files\\Java\\jre7\\bin\\java.exe"):
		java = "\"C:\\Program Files\\Java\\jre7\\bin\\java.exe\" -cp \""
	elif os.path.exists("C:\\Program Files\\Java\\jre6\\bin\\java.exe"):
		java = "\"C:\\Program Files\\Java\\jre6\\bin\\java.exe\" -cp \""
p = subprocess.Popen(java + cp + "\" edu.emory.cellbio.svg.EmbedAndCropInkscapeEntry " + f, stdout=subprocess.PIPE)
print(p.communicate()[0])
