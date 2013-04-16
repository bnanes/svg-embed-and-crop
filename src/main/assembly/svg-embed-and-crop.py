import subprocess, os.path, sys
cp = os.path.expandvars("${APPDATA}/inkscape/extensions/svg-embed-and-crop/*")
if len(sys.argv) > 1:
	f = sys.argv[1]
else:
	f = ""
p = subprocess.Popen("java -classpath \"" + cp + "\" edu.emory.cellbio.svg.EmbedAndCropInkscapeEntry " + f, stdout=subprocess.PIPE)
print(p.communicate()[0])
