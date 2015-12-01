package com.inet.gradle.appbundler;

import org.gradle.api.internal.file.FileResolver;

import com.inet.gradle.setup.AbstractBuilder;
import com.inet.gradle.setup.AbstractSetupTask;

/**
 * Create code signature for packages. Deep Signing. 
 * @author gamma
 * @param <S> concrete SetupBuilder
 *
 */
public class OSXCodeSign<S> extends AbstractBuilder<AbstractSetupTask<S>, S> {

	/**
	 * Setup up the Sign Tool
	 * @param task task
	 * @param fileResolver resolver
	 */
	public OSXCodeSign(AbstractSetupTask<S> task, FileResolver fileResolver) {
		super(task, fileResolver);
	}

}
