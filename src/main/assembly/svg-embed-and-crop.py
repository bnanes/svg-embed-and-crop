import inkex
import subprocess, os.path, sys, platform

try:
	from inkex.utils import errormsg, Boolean
	from inkex.colors import Color, ColorIdError, ColorError
	from inkex.elements import load_svg, BaseElement, ShapeElement, Group, Layer, Grid, \
					  TextElement, FlowPara, FlowDiv
	from inkex.base import InkscapeExtension, SvgThroughMixin, SvgInputMixin, SvgOutputMixin, TempDirMixin
	from inkex import command
except ImportError:
	# This is a hack for backwards compatability with Inkscape versions <1.0
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
	exit()

class EmbedAndCrop(inkex.CallExtension):
	"""Embed and Crop Images"""

	def call(self, input_file, output_file):
		cp = os.path.expandvars("${APPDATA}/inkscape/extensions/svg-embed-and-crop/*")
		java = "javaw -cp \""
		#p = subprocess.Popen(java + cp + "\" edu.emory.cellbio.svg.EmbedAndCropInkscapeEntry " + input_file, stdout=subprocess.PIPE)
		#q = p.communicate()[0]
		command.call('javaw', '-cp', cp, 'edu.emory.cellbio.svg.EmbedAndCropInkscapeEntry', input_file, "-o", output_file)
		return output_file

if __name__ == '__main__':
	EmbedAndCrop().run()
