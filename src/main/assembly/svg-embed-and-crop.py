import subprocess, os.path, sys, platform
cp = os.path.expandvars("${APPDATA}/inkscape/extensions/svg-embed-and-crop/*")
if len(sys.argv) > 1:
	f = sys.argv[1]
else:
	f = ""
java = "javaw -cp \""
if(platform.system() == "Windows"):
	if os.path.exists("C:\\Program Files\\Java\\jre7\\bin\\javaw.exe"):
		java = "\"C:\\Program Files\\Java\\jre7\\bin\\javaw.exe\" -cp \""
	elif os.path.exists("C:\\Program Files\\Java\\jre6\\bin\\javaw.exe"):
		java = "\"C:\\Program Files\\Java\\jre6\\bin\\javaw.exe\" -cp \""
p = subprocess.Popen(java + cp + "\" edu.emory.cellbio.svg.EmbedAndCropInkscapeEntry " + f, stdout=subprocess.PIPE)
print(p.communicate()[0])
