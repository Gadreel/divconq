package divconq.web.md;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;

import divconq.web.WebContext;
import divconq.web.dcui.Nodes;
import divconq.web.md.plugin.PairedMediaSection;
import divconq.web.md.plugin.StandardSection;

// TODO config should be per domain / website
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
			.setSafeMode(false)
			.registerPlugins(new PairedMediaSection(), new StandardSection());
						
			// TODO
			//.registerPlugins(new YumlPlugin(), new WebSequencePlugin(), new IncludePlugin());
		
		this.safeconfig = new Configuration();
	}
	
	public Markdown registerPlugins(Plugin ... plugins) {
		this.unsafeconfig.registerPlugins(plugins);
		return this;
	}
	
	public Nodes process(WebContext ctx, Path file) throws IOException {
		return Processor.process(ctx, file, this.unsafeconfig);
	}
	
	public Nodes process(WebContext ctx, InputStream input) throws IOException {
		return Processor.process(ctx, input, this.unsafeconfig);
	}
	
	public Nodes process(WebContext ctx, Reader reader) throws IOException {
		return Processor.process(ctx, reader, this.unsafeconfig);
	}
	
	public Nodes process(WebContext ctx, String input) throws IOException {
		return Processor.process(ctx, input, this.unsafeconfig);
	}
	
	public Nodes processSafe(WebContext ctx, Path file) throws IOException {
		return Processor.process(ctx, file, this.safeconfig);
	}
	
	public Nodes processSafe(WebContext ctx, InputStream input) throws IOException {
		return Processor.process(ctx, input, this.safeconfig);
	}
	
	public Nodes processSafe(WebContext ctx, Reader reader) throws IOException {
		return Processor.process(ctx, reader, this.safeconfig);
	}
	
	public Nodes processSafe(WebContext ctx, String input) throws IOException {
		return Processor.process(ctx, input, this.safeconfig);
	}
}
