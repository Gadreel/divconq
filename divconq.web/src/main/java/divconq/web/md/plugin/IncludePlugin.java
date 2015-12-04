package divconq.web.md.plugin;

import java.util.List;
import java.util.Map;

import divconq.web.md.Plugin;
import divconq.web.md.ProcessContext;
import divconq.xml.XElement;

public class IncludePlugin extends Plugin {

	public static final String DEFAULT_CHARSET = System.getProperty("file.encoding");

	public IncludePlugin() {
		super("include");
	}
	
	@Override
	public void emit(ProcessContext ctx, XElement parent, List<String> lines,
			Map<String, String> params) {
		// TODO Auto-generated method stub
		
	}

	/*
	@Override
	public void emit(final StringBuilder out, final List<String> lines, final Map<String, String> params) {
		String src = params.get("src");
		try {
			String content2 = getContent(src);
			if(content2 != null) {
				out.append(content2);
			}
		} catch (Exception e) {
			throw new RuntimeException("Error while rendering "+this.getClass().getName(), e);
		}
	}
	private String getContent(String src) throws IOException, URISyntaxException {
		URL url = new URL(new URL("file:"), src);		
		
		URLConnection urlc = url.openConnection();
		
        String charset = getCharsetFromContentTypeString(urlc.getHeaderField("Content-Type"));
	    if (charset == null) {
	        charset = getCharsetFromContent(url);
	    }
	    if (charset == null) {
	        charset = DEFAULT_CHARSET;
	    }
	    
	    System.err.println(charset);
		
		urlc.connect();
		InputStream is = urlc.getInputStream();
		InputStreamReader isr = new InputStreamReader(is, charset);
		BufferedReader br = new BufferedReader(isr);
		StringWriter sw = new StringWriter();
		BufferedWriter bw = new BufferedWriter(sw);
		String line = null;
		while((line = br.readLine()) != null) {
		    bw.write(line);
		    bw.newLine();
		    
		}
		br.close();
		bw.close();
	    return sw.toString();
			  
	}
//	private String resolveContentEncoding(URLConnection urlc, String charset) {
//        if (charset == null) {
//            charset = getCharsetFromContentTypeString(urlc.getHeaderField("Content-Type"));
//        }
//        if (charset == null) {
//            charset = getCharsetFromContent(url);
//        }
//        if (charset == null) {
//            charset = DEFAULT_CHARSET;
//        }
//		return charset;
//	}
    public static String getCharsetFromContentTypeString(String contentType) {
        if (contentType != null) {
            String pattern = "charset=([a-z\\d\\-]*)";
            Matcher matcher = Pattern.compile(pattern,  Pattern.CASE_INSENSITIVE).matcher(contentType);
            if (matcher.find()) {
                String charset = matcher.group(1);
                if (Charset.isSupported(charset)) {
                    return charset;
                }
            }
        }
        
        return null;
    }

    public static String getCharsetFromContent(URL url) throws IOException {
        InputStream stream = url.openStream();
        byte chunk[] = new byte[2048];
        int bytesRead = stream.read(chunk);
        if (bytesRead > 0) {
            String startContent = new String(chunk);
            String pattern = "\\<meta\\s*http-equiv=[\\\"\\']content-type[\\\"\\']\\s*content\\s*=\\s*[\"']text/html\\s*;\\s*charset=([a-z\\d\\-]*)[\\\"\\'\\>]";
            Matcher matcher = Pattern.compile(pattern,  Pattern.CASE_INSENSITIVE).matcher(startContent);
            if (matcher.find()) {
                String charset = matcher.group(1);
                if (Charset.isSupported(charset)) {
                    return charset;
                }
            }
        }

        return null;
    }
	*/
}
