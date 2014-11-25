/* ************************************************************************
#
#  DivConq
#
#  http://divconq.com/
#
#  Copyright:
#    Copyright 2014 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package divconq.test;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.joda.time.DateTime;

import divconq.filestore.CommonPath;
import divconq.io.OutputWrapper;
import divconq.lang.Memory;
import divconq.lang.chars.Utf8Decoder;
import divconq.lang.chars.Utf8Encoder;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationObserver;
import divconq.service.plugin.Operation;
import divconq.struct.CompositeParser;
import divconq.struct.CompositeStruct;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.builder.JsonMemoryBuilder;
import divconq.struct.builder.YamlStreamBuilder;
import divconq.struct.serial.CompositeToBufferBuilder;
import divconq.util.HexUtil;
import divconq.work.TaskRun;
import static divconq.struct.StructUtil.*;

public class TestYaml {

	public static void main(String[] args) throws Exception {
	}
	
	public static void maincp(String[] args) {
		CommonPath p = new CommonPath("/hi.txt");
		System.out.println("1: " + p);
		p = new CommonPath("/lie/hi.txt");
		System.out.println("2: " + p);
		//p = new CommonPath("/lie/./hi.txt");
		//System.out.println("3: " + p);
		p = new CommonPath("/lie/../hi.txt");
		System.out.println("4: " + p);
	}
	
	/*
	public static void main4(String[] args) {
		ExecutorService pool = Executors.newSingleThreadExecutor();
		
		ResourceLeakDetector.setLevel(Level.PARANOID);
		
		Logger.init(null);
		
		FileSystemDriver localfs = new FileSystemDriver();
		localfs.setRootFolder("C:/");
		
		FileCollection files = new FileCollection();
		files.add(localfs.getReference("/Users/andy/Documents/writing/buck2/story.xml"));
		files.add(localfs.getReference("/GreasySpoon/conf/mime.types"));
		
		FileSourceStream src = new FileSourceStream(pool, files);
		TarStream tar = new TarStream();
		GzipStream gz = new GzipStream();
		FileDestStream dest = new FileDestStream(localfs.getReference("/temp/test/files.tar.gz"));
		
		tar.setUpstream(src);
		gz.setUpstream(tar);
		dest.setUpstream(gz);
		
		dest.execute(new OperationCallback() {			
			@Override
			public void callback() {

				System.out.println("Done");
			}
		});
		
		try (Scanner scan = new Scanner(System.in)) {
			System.out.println("Press enter to close");
			scan.nextLine();
		}
		
		Logger.stop(new OperationResult());
		pool.shutdown();
	}
	*/
	
	public static void main3(String[] args) {		
		// create records, fields and lists easily
		RecordStruct rec = record(
				field("Name", "Fred"),
				field("Age", 49),
				field("FavoriteFoods", list("pizza", "cereal", "lima beans"))
		);
		
		// output as JSON
		rec.toString();
		
		// output as formatted JSON
		rec.toPrettyString();
		
		// validate the structure follows the schema
		rec.validate("dcTestPeopleExample");
	}

	public static void main2(String[] args) {
		FuncResult<CompositeStruct> jres = CompositeParser.parseJson("{ one: 'has', two: \"is\", three: [ 12, 44, 88 ], four: 12.55, five: false, six: null }");
		
		if (jres.hasErrors())
			System.out.println("Error: " + jres.getMessage());
		else {
			RecordStruct out = (RecordStruct)jres.getResult();
			
			System.out.println("= " + out.toPrettyString());
			
			System.out.println("a: " + out.getFieldAsDecimal("four"));
			System.out.println("b: " + out.getFieldAsString("two"));
			System.out.println("c: " + out.getFieldAsBoolean("five"));
			System.out.println("d: " + out.getFieldAsList("three").getItemAsInteger(2));
		}		
	}
    
	public static void mainGroovService(String[] args) throws Exception {
		System.out.println("dude!");

		RecordStruct rec = new RecordStruct(
				new FieldStruct("Age", 15)
		);
		
		GroovyClassLoader loader = new GroovyClassLoader();
		Class<?> groovyClass = loader.parseClass(new File("./packages/dcTest/services/FirstGroovy.groovy"));
		
		for (Method m : groovyClass.getMethods()) {
			Operation ann = m.getAnnotation(Operation.class);
			
			// to be a service operation you need to have an annotation and name starts with "handle" 
			if ((ann == null) || !m.getName().startsWith("handle"))
				continue;
			
			System.out.println("Service handler: " + m.getName().substring(6) + " request type: " + ann.request().type());
		}
		
		/*
		// let's call some method on an instance
		GroovyObject groovyObject = (GroovyObject) groovyClass.newInstance();
		Object[] args2 = { rec };
		
		//for (MetaMethod m : groovyObject.getMetaClass().getMethods()) {
		//	System.out.println("M1: " + m.getName());
		//	System.out.println("M3: " + m.getSignature());
		//	System.out.println("M3: " + m.getDescriptor());
		//}
		
		@SuppressWarnings("unchecked")
		Closure<Object> res = (Closure<Object>)groovyObject.invokeMethod("printIt", args2);		
		
		res.call();
		*/
		
		TaskRun run = new TaskRun();
		
		run.getTask().withObserver(new OperationObserver() {
			@Override
			public void completed(OperationContext or) {
				System.out.println("Result of run: " + or.getTaskRun().getResult());
			}
		});
		
		GroovyObject groovyObject = (GroovyObject) groovyClass.newInstance();
		Object[] args2 = { run, "tee shirt" };
		
		groovyObject.invokeMethod("handleEcho", args2);		
		
		System.out.println("after: " + rec.getFieldAsInteger("Age"));
		
		Thread.sleep(5000);
		
		loader.close();
	}
	
	public static void mainJS(String[] args) throws Exception {
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
		
		engine.eval(new FileReader("./packages/dcTest/scripts/hello1.js"));
		
		Invocable invocable = (Invocable) engine;

		RecordStruct rec = new RecordStruct(
				new FieldStruct("Age", 15)
		);
		
		Object result = invocable.invokeFunction("fun1", "Peter Parker", rec);
		System.out.println(result);
		//System.out.println(result.getClass());
		
		System.out.println("after: " + rec.getFieldAsInteger("Age"));
		
		Thread.sleep(5000);
		
		System.out.println("after: " + rec.getFieldAsInteger("Age"));
	}
	
	static public Memory getRemote(String url) {
		try {
			Memory res = new Memory();
			
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
	 
			// optional default is GET
			con.setRequestMethod("GET");
	 
			//add request header
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
	 
			//System.out.println("\nSending 'GET' request to URL : " + url);
			int responseCode = con.getResponseCode();
			
			//System.out.println("Response Code : " + responseCode);
			
			if (responseCode != 200)
				return null;

			InputStream is = con.getInputStream();
			
			res.copyFromStream(is);
			
			is.close();
			
			return res;		
		}
		catch (Exception x) {
			System.out.println("Error reading url: " + url + " - " + x);
		}
		
		return null;
	}
	
	
	static public void realmain() {
		/* TODO
		try {
			CharBuffer cb = CharBuffer.allocate(1024);
			InputStreamReader sr = new InputStreamReader(System.in, "UTF-8");
			
			sr.read(cb);
	
			String u = "0ä0"; 
			
			cb.flip();
			
			System.out.println("= " + cb.equals(u));
		}
		catch (Exception x) {
			System.out.println("Error: " + x);
		}
		*/
		
		
		String u = "avä73Dw??gT80Hgt"; 
		
		System.out.println(u);
		
		byte[] xb = Utf8Encoder.encode(u);
		
		System.out.println(HexUtil.bufferToHex(xb));  // 6176c3a4373344773f3f67543830486774
														 
		System.exit(0);
		
		CommonPath ptest = new CommonPath("jump/and/stomp");
		
		System.out.println("1: " + ptest.getFileName());
		System.out.println("2: " + ptest.getName(0));
		System.out.println("3: " + ptest.getName(1));
		System.out.println("4: " + ptest.getName(2));
		System.out.println("5: " + ptest.getParent());
		System.out.println("6: " + ptest.subpath(0, 1));
		
		ptest = new CommonPath("jump");
		
		System.out.println("1: " + ptest.getFileName());
		System.out.println("2: " + ptest.getName(0));
		System.out.println("5: " + ptest.getParent());
		System.out.println("6: " + ptest.subpath(0, 1));
		
		ptest = new CommonPath("/jump");
		
		System.out.println("1: " + ptest.getFileName());
		System.out.println("2: " + ptest.getName(0));
		System.out.println("5: " + ptest.getParent());
		System.out.println("6: " + ptest.subpath(0, 1));
		
		ptest = new CommonPath("/jump/and/stomp");
		
		System.out.println("1: " + ptest.getFileName());
		System.out.println("2: " + ptest.getName(0));
		System.out.println("3: " + ptest.getName(1));
		System.out.println("4: " + ptest.getName(2));
		System.out.println("5: " + ptest.getParent());
		System.out.println("6: " + ptest.subpath(0, 1));

		System.exit(0);
		
    	Path file = Paths.get("..", "lib", "aws-java-sdk-1.3.21.1.jar");
        
        if (!Files.exists(file)) {
            System.out.println("File not found: " + file);
        	return;
        }
        
        if (!Files.isRegularFile(file)) {
        	System.out.println("Not a file: " + file);
            return;
        }
            
        System.out.println("file good");
        
        final CountDownLatch latch = new CountDownLatch(1);
        
        try {
        	AsynchronousFileChannel sbc = AsynchronousFileChannel.open(file);
            final ByteBuffer buf = ByteBuffer.allocate(64 * 1024);

            sbc.read(buf, 0, sbc, new CompletionHandler<Integer, AsynchronousFileChannel>() {
            	long pos = 0;
            	
				@Override
				public void completed(Integer result, AsynchronousFileChannel sbc) {
					if (result == -1) {
						try {
							sbc.close();
						} 
						catch (IOException x) {
						}
						
						latch.countDown();
						
						return;
					}

					if (result > 0) {
						this.pos += result;
			            
				        //ctx.writeAndFlush(new StreamMessage(buf));
						System.out.println("get: " + buf.position());						
				        
				        buf.clear();
					}
					
					sbc.read(buf, this.pos, sbc, this);
				}

				@Override
				public void failed(Throwable x, AsynchronousFileChannel sbc) {
					// TODO logging
					
					System.out.println("error: " + x);
					
					try {
						sbc.close();
					} 
					catch (IOException x2) {
					}
					
					latch.countDown();
				}
			});
        } 
        catch (IOException x) {
            System.out.println("Server Stream failed to open file: " + x);
        }
		
        try {
			latch.await();
		} 
        catch (Exception x) {
		}
        
		//TestYaml.testSerial();
	}
	
	public static void testSerial() {
		ByteBuf bb = Unpooled.buffer();
		
		CompositeToBufferBuilder sb = new CompositeToBufferBuilder(bb);
		
		try {
			/*
			sb.startList();
			
			sb.startList();
			sb.value("a");
			//sb.value(null);
			//sb.value("b");
			sb.endList();
			
			sb.value("c");

			sb.startList();
			sb.value(null);
			sb.endList();
			
			sb.endList();
			*/
			
			sb.startList();
			
			sb.startList();

			sb.startList();
			sb.value("a");
			sb.value("b");
			sb.endList();

			sb.value(null);
			sb.value(22);
			sb.value(2.2);
			
			sb.startList();
			sb.value(2.2);
			sb.value(null);
			sb.value(22);
			sb.endList();

			sb.endList();
			
			sb.startRecord();
			sb.field("Name", "ted");
			sb.field("Age", 7);
			sb.field("DOB", new DateTime(1998, 8, 3, 17, 12));
			sb.field("Sex", "m");
			sb.field("Software");
			sb.startList();
			sb.value("Afterstep <& or \"");
			sb.value("CTWM this is an abcdefghijklmnopqrstuvwxy is an abcdefghijklmnopqrstuvwxy is /> an abcdefghijklmnopqrstuvwxy is \nan abcdefghijklmnopqrstuvwxy is \tan abcdefghijklmnopqrstuvwxy");
			sb.value("Oroborus");
			sb.endList();
			sb.field("Friends");
			sb.startList();
			
			sb.startRecord();
			sb.field("Name", "macy");
			sb.field("Age", 6);
			sb.field("Sex", "f");
			sb.endRecord();
			
			sb.startRecord();
			sb.field("Name", "rich");
			sb.field("Age", 7);
			sb.field("Sex", "m");
			sb.field("Foods");
			sb.startList();
			sb.value("Apple");
			sb.value("Cookie");
			sb.endList();
			sb.endRecord();
			
			sb.endList();
			sb.endRecord();
			
			sb.startRecord();
			sb.field("Name", "kathy");
			sb.field("Father");
			sb.startRecord();
			sb.field("Name", "Ted");
			sb.field("Age", 29);
			sb.endRecord();
			sb.field("Age");
			sb.field("DOB", new DateTime(1994, 3, 13, 8, 55));
			sb.field("Sex", "f");
			sb.endRecord();
			
			sb.endList();
		}
		catch (Exception x) {
			System.out.println("builder error: " + sb);
		}

		System.out.println("built:\n" + Utf8Decoder.decode(bb));
		
		ListStruct list = (ListStruct) sb.toLocal();
		
		System.out.println();
		System.out.println("item 2 name: " +  list.getItemAsRecord(1).getFieldAsString("Name"));
		//System.out.println("item 2: " +  list.getItem(1));
		System.out.println();
		
		System.out.println(list.toPrettyString());
		
		System.exit(0);
	}
	
	public static void testJson() {
		JsonMemoryBuilder sb = new JsonMemoryBuilder(true);
		
		try {
			sb.startList();
			
			sb.startList();

			sb.startList();
			sb.value("a");
			sb.value("b");
			sb.endList();

			sb.value(null);
			sb.value(22);
			sb.value(2.2);
			
			sb.startList();
			sb.value(2.2);
			sb.value(null);
			sb.value(22);
			sb.endList();

			sb.endList();
			
			
			sb.startRecord();
			sb.field("Name", "ted");
			sb.field("Age", 7);
			sb.field("DOB", new DateTime(1998, 8, 3, 17, 12));
			sb.field("Sex", "m");
			sb.field("Software");
			sb.startList();
			sb.value("Afterstep <& or \"");
			sb.value("CTWM this is an abcdefghijklmnopqrstuvwxy is an abcdefghijklmnopqrstuvwxy is /> an abcdefghijklmnopqrstuvwxy is \nan abcdefghijklmnopqrstuvwxy is \tan abcdefghijklmnopqrstuvwxy");
			sb.value("Oroborus");
			sb.endList();
			sb.field("Friends");
			sb.startList();
			
			sb.startRecord();
			sb.field("Name", "macy");
			sb.field("Age", 6);
			sb.field("Sex", "f");
			sb.endRecord();
			
			sb.startRecord();
			sb.field("Name", "rich");
			sb.field("Age", 7);
			sb.field("Sex", "m");
			sb.field("Foods");
			sb.startList();
			sb.value("Apple");
			sb.value("Cookie");
			sb.endList();
			sb.endRecord();
			
			sb.endList();
			sb.endRecord();
			
			sb.startRecord();
			sb.field("Name", "kathy");
			sb.field("Father");
			sb.startRecord();
			sb.field("Name", "Ted");
			sb.field("Age", 29);
			sb.endRecord();
			sb.field("Age");
			sb.field("DOB", new DateTime(1994, 3, 13, 8, 55));
			sb.field("Sex", "f");
			sb.endRecord();
			
			sb.endList();
		}
		catch (Exception x) {
			System.out.println("builder error: " + sb);
		}

		System.out.println("built:\n" + sb.getMemory());
		
		ListStruct list = (ListStruct) sb.toLocal();
		
		System.out.println();
		System.out.println("item 2 name: " +  list.getItemAsRecord(1).getFieldAsString("Name"));
		System.out.println();
		
		System.out.println(list.toString());
		
		System.exit(0);
	}
	
	public static void testYaml() {
		Memory ydest = new Memory();
		OutputWrapper os = new OutputWrapper(ydest);
		YamlStreamBuilder sb = new YamlStreamBuilder(new PrintStream(os));
		
		try {
			sb.startList();
			
			sb.startList();

			sb.startList();
			sb.value("a");
			sb.value("b");
			sb.endList();

			sb.value(null);
			sb.value(22);
			sb.value(2.2);
			
			sb.startList();
			sb.value(2.2);
			sb.value(null);
			sb.value(22);
			sb.endList();

			sb.endList();
			
			
			sb.startRecord();
			sb.field("Name", "ted");
			sb.field("Age", 7);
			sb.field("DOB", new DateTime(1998, 8, 3, 17, 12));
			sb.field("Sex", "m");
			sb.field("Software");
			sb.startList();
			sb.value("Afterstep <& or \"");
			sb.value("CTWM this is an abcdefghijklmnopqrstuvwxy is an abcdefghijklmnopqrstuvwxy is /> an abcdefghijklmnopqrstuvwxy is \nan abcdefghijklmnopqrstuvwxy is \tan abcdefghijklmnopqrstuvwxy");
			sb.value("Oroborus");
			sb.endList();
			sb.field("Friends");
			sb.startList();
			
			sb.startRecord();
			sb.field("Name", "macy");
			sb.field("Age", 6);
			sb.field("Sex", "f");
			sb.endRecord();
			
			sb.startRecord();
			sb.field("Name", "rich");
			sb.field("Age", 7);
			sb.field("Sex", "m");
			sb.field("Foods");
			sb.startList();
			sb.value("Apple");
			sb.value("Cookie");
			sb.endList();
			sb.endRecord();
			
			sb.endList();
			sb.endRecord();
			
			sb.startRecord();
			sb.field("Name", "kathy");
			sb.field("Father");
			sb.startRecord();
			sb.field("Name", "Ted");
			sb.field("Age", 29);
			sb.endRecord();
			sb.field("Age");
			sb.field("DOB", new DateTime(1994, 3, 13, 8, 55));
			sb.field("Sex", "f");
			sb.endRecord();
			
			sb.endList();
		}
		catch (Exception x) {
			System.out.println("builder error: " + sb);
		}

		System.out.println("built:\n" + ydest);
		
		//String yaml = "---\nboolean: !!bool \"true\"\ninteger: !!int \"3\"\nfloat: !!float \"3.14\"\nnull: !!null\nstr: !!str \"abc\"\n"
		//		+ "---\n- Afterstep\n- CTWM\n- Oroborus";

		//String yaml = "datetime: !!timestamp 2009-12-09T08:35:45.000Z\nboolean: !!bool \"true\"\ninteger: !!int \"3\"\nfloat: !!float \"3.14\"\nnull: !!null\nstr: !!str \"abc\"\n";

		ydest.setPosition(0);
		
		System.out.println();
		
		//System.out.println(CompositeParser.parseYaml(ydest).toString());
		
		System.exit(0);
	}
}
