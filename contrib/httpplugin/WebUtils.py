from org.python.modules import re
from javax.swing import JOptionPane
from net.grinder.plugin.http import HTTPPluginControl
from HTTPClient import CookiePolicyHandler, Cookie, Cookie2

# Cookie Policy Handler
# CookieModule.setCookiePolicyHandler(MyCookiePolicyHandler(jsess, 'JSESSIONID_ECH'))
class MyCookiePolicyHandler(CookiePolicyHandler):
	# constructor
	# jsess: new value for the cookie named <cookieName>
	# log: grinder.logger.output
	# debug: debug information or not
	def __init__ (self, jsess, cookieName, log, debug=0):
		self.jsess = jsess
		self.cookieName = cookieName
		self.log = log
		self.debug = debug

	def acceptCookie(self, cookie, request, response):
		version = ""
		if ("%s" % cookie.getClass() == "HTTPClient.Cookie"):
			version = "1"
		if ("%s" % cookie.getClass() == "HTTPClient.Cookie2"):
			version = "2"

		if (cookie.getName() == self.cookieName):
			if (version == "1"):
				cookie = Cookie (cookie.getName(), self.jsess, cookie.getDomain(), cookie.getPath(), cookie.expires(), cookie.isSecure())
			if (version == "2"):
				cookie = Cookie2 (cookie.getName(), self.jsess, cookie.getDomain(), cookie.getPorts(), cookie.getPath(), cookie.expires(), cookie.discard(), cookie.isSecure(), cookie.getComment(), cookie.getCommentUrl())
		if (self.debug):
			self.log ("Cookie: %s" % cookie)
		return 1

	def sendCookie(self, cookie, request):
		if (self.debug):
			self.log ("Cookie: %s" % cookie)
		return 1

# save the HTML response to a file
def saveHtmlToFile (prefix, response, grinder):
	inputStream = response.getInputStream()
	filename = grinder.getFilenameFactory().createFilename(prefix + "_page", "-%d.html" % grinder.runNumber)
	file = open(filename, "w")
	i = 1
	taille = inputStream.available()
	while (i <= taille):
		c = inputStream.read()
		file.write("%c" % c)
		i += 1
		
	inputStream.close()
	file.close()
	return filename

# get HTML response size
def getHtmlResponseSize (response):
	inputStream = response.getInputStream()
	size = inputStream.available()
	inputStream.close()
	return size

# check response for wrong status code or error page
def checkResponse (HttpResponse, grinder, doLogging=0):
	patternError = re.compile("problème technique momentané")
	patternTimeout = re.compile("Votre session")
	patternAccessDenied = re.compile("Accès refusé")
	
	grinder.logger.output ("Checking response...")
	if HttpResponse.getStatusCode() >= 400:
		grinder.logger.output ('Error status bigger than 400 (client or server)')
		raise Exception ("HTML ERROR %s" % HttpResponse.getStatusCode())
	
	if patternError.search(HttpResponse.text, 0):
		grinder.logger.output ('Error : got PortalError.jsp')
		if doLogging == 1:
			saveHtmlToFile ("ERROR", HttpResponse, grinder)
		
		raise Exception ("BUSINESS ERROR")
	
	if patternTimeout.search(HttpResponse.text, 0):
		grinder.logger.output ('Error : got SessionTimeout.jsp')
		if doLogging == 1:
			saveHtmlToFile ("SESSION TIMEOUT", HttpResponse, grinder)
		
		raise Exception ("SESSION TIMEOUT")
	
	if patternAccessDenied.search(HttpResponse.text, 0):
		grinder.logger.output ('Error : got AccessDenied.jsp')
		if doLogging == 1:
			saveHtmlToFile ("ACCESS DENIED", HttpResponse, grinder)
		
		raise Exception ("ACCESS DENIED")

# extract JSESSION
def extractJSESSION(jsessname, HttpResponse, grinder, doLogging=0):
	try:
		pattern = re.compile(jsessname + "=[-a-zA-Z0-9!]+")
		target = HttpResponse.text
		result2 = pattern.search(target, 0)
		value = result2.group(0)
		value = value[15:]
		assert len(value) > 0
		if doLogging == 1:
			grinder.logger.output ("Jsession value: %s" % value)
		
		return value
	except:
		grinder.logger.output ('Error: in function extractJSESSION')
		raise Exception ("Error: in function extractJSESSION")

# extract Html links
# TODO replace JSESSIONID_ECH
# TODO delete SiteRoot
def extractFromHtml(SiteRoot, HTMLResponse, regex):
    # regex to enhanced: delimiter
	linkMatch = re.compile(regex)
	linksList = linkMatch.findall (HTMLResponse.text)
	
	return linksList
	
def extractVariable (HTMLResponse, variableName, variableType):
	# TODO here
	return {variableName:"1000"}

# log a list
def logList (aList, grinder):
	for link in aList:
		grinder.logger.output(">> LINK: %s" % link)

# log a dictionary
def logDict (aDict, grinder):
	for k, v in aDict.items():
			grinder.logger.output (">> %s: %s" % (k, v))
	
# discard duplicates
def discardDuplicates (aList):
	u = {}
	try:
		for x in aList:
			u[x] = 1
	except TypeError:
		del u
	else:
		return u.keys()

# session timeout exception
def SessionTimeoutException(Exception):
	# constructor
	def __init__ (self):
		super("SESSION TIMEOUT")