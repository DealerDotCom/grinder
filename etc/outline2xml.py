#!/usr/bin/python

# Inspiration drawn from o2x (http://www.sabren.com/code/python/)

class ElementStack:
    _data = []

    def open(self, tag, **attributes):
        self._data.append(tag)
        result = "<%s" % tag
        for key,value in attributes.items():
            result += " %s='%s'" % (key.replace("_", ":"), value)
        return result + ">\n"

    def ensureOpen(self, tag):
        if self._data[-1] != tag:
            return self.open(tag)
        else:
            return ""

    def perhapsClose(self, tag):
        if self._data[-1] == tag:
            return self.close()
        else:
            return ""

    def close(self):
        tag  = self._data.pop()
        return "</%s>\n" % tag

    def depth(self):
        return len(self._data)

def outline2xml(text):
    stack = ElementStack()
    forceParagraph = 1

    result = '<?xml version="1.0"  encoding="iso-8859-1"?>\n\n'
    result += stack.open("todo")
    
    for line in text.split("\n"):
        if line[:3] == "-*-": continue
        
        line = line.replace("&", "&amp;");
        line = line.replace("<", "&lt;");
        line = line.replace(">", "&gt;");

        if line and line[0] == "*":

            depth = 0
            while line[depth] == "*": depth = depth + 1

            for difference in range(stack.depth(), depth):
                result += stack.open("section", depth=difference)

            for difference in range(depth, stack.depth()):
                result += stack.close()
				
            result += stack.open("section",
                                 name = line[depth:].strip(),
                                 depth = depth)
            forceParagraph = 1
        else:
            if not line:
                forceParagraph = 1
            else:
                if forceParagraph:
                    forceParagraph = 0
                    result += stack.perhapsClose("p")
                    result += stack.ensureOpen("p")

                if line[0] == " " or line[0] == "\t":
                    result += "<br/>"
                    result += line
                else:
                    result += line
                result += " "
			
    while 1:
        try:
            result += stack.close()
        except IndexError:
            break

    return result



if __name__ == "__main__":
    import sys
    print outline2xml(open(sys.argv[1], "r").read())
