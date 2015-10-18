package divconq.web.md;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;

import divconq.web.WebContext;
import divconq.web.dcui.Nodes;
import divconq.web.md.process.Configuration;
import divconq.web.md.process.Plugin;
import divconq.web.md.process.Processor;;

// TODO config should be per domain
public class Markdown {
	protected Configuration unsafeconfig = null;
	protected Configuration safeconfig = null;
	
	public Configuration getUnsafeConfig() {
		return this.unsafeconfig;
	}
	
	public Configuration getSafeConfig() {
		return this.safeconfig;
	}
	
	public Markdown() {
		this.unsafeconfig = new Configuration()
						.setSafeMode(false);
						// TODO
						//.registerPlugins(new YumlPlugin(), new WebSequencePlugin(), new IncludePlugin());
		
		this.safeconfig = new Configuration();
	}
	
	public Markdown registerPlugins(Plugin ... plugins) {
		this.unsafeconfig.registerPlugins(plugins);
		return this;
	}
	
	public void process(WebContext ctx, Nodes nodes, Path file) throws IOException {
		Processor.process(ctx, nodes, file, this.unsafeconfig);
	}
	
	public void process(WebContext ctx, Nodes nodes, InputStream input) throws IOException {
		Processor.process(ctx, nodes, input, this.unsafeconfig);
	}
	
	public void process(WebContext ctx, Nodes nodes, Reader reader) throws IOException {
		Processor.process(ctx, nodes, reader, this.unsafeconfig);
	}
	
	public void process(WebContext ctx, Nodes nodes, String input) throws IOException {
		Processor.process(ctx, nodes, input, this.unsafeconfig);
	}
	
	public void processSafe(WebContext ctx, Nodes nodes, Path file) throws IOException {
		Processor.process(ctx, nodes, file, this.safeconfig);
	}
	
	public void processSafe(WebContext ctx, Nodes nodes, InputStream input) throws IOException {
		Processor.process(ctx, nodes, input, this.safeconfig);
	}
	
	public void processSafe(WebContext ctx, Nodes nodes, Reader reader) throws IOException {
		Processor.process(ctx, nodes, reader, this.safeconfig);
	}
	
	public void processSafe(WebContext ctx, Nodes nodes, String input) throws IOException {
		Processor.process(ctx, nodes, input, this.safeconfig);
	}
}
