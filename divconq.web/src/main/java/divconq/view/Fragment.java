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
package divconq.view;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import divconq.lang.OperationCallback;
import divconq.web.ViewInfo;

public class Fragment extends Element {
	protected ViewInfo view = null;

	protected AtomicInteger hasfuture = null;
	protected List<OperationCallback> callbacks = null;

	public ViewInfo getView() {
		return this.view;
	}

	public void setViewInfo(ViewInfo info) {
		this.view = info;
	}

	public Fragment(ViewInfo info) {
		super();
		this.view = info;
		this.viewroot = this;
		this.partroot = this;
	}

	@Override
	public Node deepCopy(Element parent) {
		Fragment cp = new Fragment(null);
		cp.setParent(parent);
		this.doCopy(cp); // no need to override, we don't care about view field
		return cp;
	}

	@Override
	public void setParent(Element value) {
		this.parent = value;
		this.viewroot = value.getViewRoot();
	}

	@Override
	public void doBuild() {
		// nothing to do, doc element is special in that it calls it's own
		// Build...
	}

	public void write() throws IOException {
		this.stream(this.getContext().getResponse().getPrintStream(), "", false, true);
	}

	@Override
	public void stream(PrintStream strm, String indent, boolean firstchild,
			boolean fromblock) {
		if (this.children.size() == 0)
			return;

		boolean fromon = fromblock;
		boolean lastblock = false;
		boolean firstch = this.getBlockIndent(); // only true once, and only if
													// bi

		for (Node node : this.children) {
			if (node.getBlockIndent() && !lastblock && !fromon)
				this.print(strm, "", true, "");

			node.stream(strm, indent, (firstch || lastblock),
					this.getBlockIndent());

			lastblock = node.getBlockIndent();
			firstch = false;
			fromon = false;
		}
	}

	public void incrementFuture() {
		synchronized (this) {
			if (this.hasfuture == null)
				this.hasfuture = new AtomicInteger();

			this.hasfuture.incrementAndGet();
		}
	}

	public void decrementFuture() {
		synchronized (this) {
			if (this.hasfuture == null)
				return;

			int cnt = this.hasfuture.decrementAndGet();

			if (cnt == 0) {
				if (this.callbacks != null) {
					for (OperationCallback cb : this.callbacks)
						cb.completed();
				}
			}
		}
	}

	public void awaitForFutures(OperationCallback cb) {
		// at this point it is too late to register any new futures (see
		// increment above)
		// it is safe to callback immediately if there was no futures
		if (this.hasfuture == null) {
			cb.completed();
			return;
		}

		synchronized (this) {
			if (this.hasfuture.get() == 0) {
				cb.completed();
				return;
			}

			if (this.callbacks == null)
				this.callbacks = new ArrayList<OperationCallback>();

			this.callbacks.add(cb);
		}
	}
}
