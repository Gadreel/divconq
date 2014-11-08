package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

import divconq.log.slf4j.LoggerFactory;

public class StaticLoggerBinder implements LoggerFactoryBinder {
	/**
	 * The unique instance of this class.
	 */
	private static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

	/**
	 * Return the singleton of this class.
	 * 
	 * @return the StaticLoggerBinder singleton
	 */
	public static final StaticLoggerBinder getSingleton() {
		return SINGLETON;
	}
	  
	static public final LoggerFactory instance = new LoggerFactory();
	
	@Override
	public ILoggerFactory getLoggerFactory() {
		return StaticLoggerBinder.instance;
	}

	@Override
	public String getLoggerFactoryClassStr() {
		return LoggerFactory.class.getName();
	}

}
