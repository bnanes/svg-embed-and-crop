import inkex
import subprocess, os.path, sys, platform

try:
	from inkex.utils import errormsg, Boolean, CloningVat, PY3
	from inkex.colors import Color, ColorIdError, ColorError
	from inkex.elements import load_svg, BaseElement, ShapeElement, Group, Layer, Grid, \
					  TextElement, FlowPara, FlowDiv
	from inkex.base import InkscapeExtension, SvgThroughMixin, SvgInputMixin, SvgOutputMixin, TempDirMixin
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

if PY3:
	unicode = str  # pylint: disable=redefined-builtin,invalid-name

class EmbedAndCrop(inkex.CallExtension):
	"""Embed and Crop Images"""

	def load_raw(self):
		# Don't call InputExtension.load_raw
		TempDirMixin.load_raw(self)
		input_file = self.options.input_file

		if not isinstance(input_file, (unicode, str)):
			data = input_file.read()
			input_file = os.path.join(self.tempdir, 'input.' + self.input_ext)
			with open(input_file, 'wb') as fhl:
				fhl.write(data)

		output_file = os.path.join(self.tempdir, 'output.' + self.output_ext)
		document = self.call(input_file, output_file) or output_file
		if isinstance(document, (str, unicode)):
			if not os.path.isfile(document):
				raise IOError("Can't find generated document: {}".format(document))
			# This is a hack to allow compatability with UTF-8 encoding
			"""if self.output_ext == 'svg':
				with open(document, 'r') as fhl:
					document = fhl.read()
				if '<' in document:
					document = load_svg(document)
			else:
				with open(document, 'rb') as fhl:
					document = fhl.read()"""
			with open(document, 'rb') as fhl:
				document = fhl.read()

		self.document = document

	def call(self, input_file, output_file):
		cp = os.path.expandvars("${APPDATA}/inkscape/extensions/svg-embed-and-crop/*")
		java = "javaw -cp \""
		p = subprocess.Popen(java + cp + "\" edu.emory.cellbio.svg.EmbedAndCropInkscapeEntry " + input_file + " -o " + output_file, stdout=subprocess.PIPE)
		q = p.communicate()[0]

if __name__ == '__main__':
	EmbedAndCrop().run()
