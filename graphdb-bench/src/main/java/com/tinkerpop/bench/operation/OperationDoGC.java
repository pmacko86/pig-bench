package com.tinkerpop.bench.operation;

/**
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 */
public class OperationDoGC extends Operation {

	@Override
	protected void onInitialize(Object[] args) {
	}

	@Override
	protected void onExecute() throws Exception {
		try {
			System.gc();
			System.gc();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
			System.gc();
			System.gc();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
			/*System.gc();
			System.gc();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
			System.gc();
			System.gc();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
			System.gc();
			System.gc();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}*/
			setResult("DONE");
		} catch (Exception e) {
			throw e;
		}
	}

}
