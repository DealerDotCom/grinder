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
	
# check response for wrong status code or error page	
def checkResponse (HttpResponse, grinder):
	patternError = re.compile("problème technique momentané")
	patternTimeout = re.compile("Votre session")
	patternAccessDenied = re.compile("AccessDenied.jsp")
	
	grinder.logger.output ("Checking response...")
	if HttpResponse.getStatusCode() >= 400:
		grinder.logger.output ('Error status bigger than 400 (client or server)')
		return 'ERROR'
	
	if patternError.search(HttpResponse.text, 0):
		grinder.logger.output ('Error : got PortalError.jsp')
		return 'ERROR'
	
	if patternTimeout.search(HttpResponse.text, 0):
		grinder.logger.output ('Error : got SessionTimeout.jsp')
		return 'ERROR'
	
	if patternAccessDenied.search(HttpResponse.getEffectiveURI().toString(), 0):
		grinder.logger.output ('Error : got AccessDenied.jsp')
		return 'ERROR'

# extract 
def extractJSESSION(jsessname, HttpResponse, grinder):
	try:
		pattern = re.compile(jsessname + "=[-a-zA-Z0-9!]+")
		target = HttpResponse.text
		result2 = pattern.search(target, 0)
		value = result2.group(0)
		value = value[15:]
		return value
	except:
		grinder.logger.output ('Error: in function extractJSESSION')
		return 'ERROR'	

# extract Html links
# TODO replace JSESSIONID_ECH
def extractHtmlLinks(SiteRoot, HTMLResponse, prefix, keyword):
	linkMatch = re.compile("href='(" + prefix + ".*?)'") # regex to enhanced: delimiter
	linksList = linkMatch.findall (HTMLResponse.text)
	
	return linksList
	
def extractHtmlImgs(SiteRoot, HTMLResponse, prefix, keyword):
	linkMatch = re.compile("src='(" + prefix + ".*?)'") # regex to enhanced: delimiter
	linksList = linkMatch.findall (HTMLResponse.text)
	
	return linksList
	
def extractVariable (HTMLResponse, variableName, variableType):
	# TODO here
	return {variableName:"1000"}

def containsItem (HTMLResponse, item):
	itemList = extractHtmlImgs ("", HTMLResponse, item, "")	
	return len(itemList)
	
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
