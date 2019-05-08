/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.securityadmin;

import ext.framework.b.TestCondition;
import java.io.File;
import java.io.FilePermission;
import java.net.SocketPermission;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.launch.Equinox;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.eclipse.osgi.tests.bundles.AbstractBundleTests;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.permissionadmin.PermissionInfo;

public class SecurityAdminUnitTests extends AbstractBundleTests {

	private static final PermissionInfo[] SOCKET_INFOS = new PermissionInfo[] {new PermissionInfo("java.net.SocketPermission", "localhost", "accept")}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	private static final PermissionInfo[] READONLY_INFOS = new PermissionInfo[] {new PermissionInfo("java.io.FilePermission", "<<ALL FILES>>", "read")}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	private static final PermissionInfo[] READWRITE_INFOS = new PermissionInfo[] {
			// multiple permission infos
			new PermissionInfo("java.io.FilePermission", "<<ALL FILES>>", "read"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			new PermissionInfo("java.io.FilePermission", "<<ALL FILES>>", "write") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	};

	private static final PermissionInfo[] RUNTIME_INFOS = new PermissionInfo[] {new PermissionInfo("java.lang.RuntimePermission", "exitVM", null)}; //$NON-NLS-1$ //$NON-NLS-2$

	private static final ConditionInfo[] ALLLOCATION_CONDS = new ConditionInfo[] {new ConditionInfo("org.osgi.service.condpermadmin.BundleLocationCondition", new String[] {"*"})}; //$NON-NLS-1$ //$NON-NLS-2$
	private static final ConditionInfo MUT_SAT = new ConditionInfo("ext.framework.b.TestCondition", new String[] {"MUT_SAT", "true", "false", "true"}); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	private static final ConditionInfo NOT_MUT_SAT = new ConditionInfo("ext.framework.b.TestCondition", new String[] {"NOT_MUT_SAT", "false", "false", "true"}); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	private static final ConditionInfo POST_MUT_SAT = new ConditionInfo("ext.framework.b.TestCondition", new String[] {"POST_MUT_SAT", "true", "true", "true"}); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	private static final ConditionInfo POST_MUT_UNSAT = new ConditionInfo("ext.framework.b.TestCondition", new String[] {"POST_MUT_UNSAT", "true", "true", "false"}); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

	private static final ConditionInfo SIGNER_CONDITION1 = new ConditionInfo("org.osgi.service.condpermadmin.BundleSignerCondition", new String[] {"*;cn=test1,c=US"}); //$NON-NLS-1$//$NON-NLS-2$
	private static final ConditionInfo SIGNER_CONDITION2 = new ConditionInfo("org.osgi.service.condpermadmin.BundleSignerCondition", new String[] {"*;cn=test2,c=US"}); //$NON-NLS-1$//$NON-NLS-2$
	private static final ConditionInfo NOT_SIGNER_CONDITION1 = new ConditionInfo("org.osgi.service.condpermadmin.BundleSignerCondition", new String[] {"*;cn=test1,c=US", "!"}); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	private static final String TEST_BUNDLE = "test"; //$NON-NLS-1$
	private static final String TEST2_BUNDLE = "test2"; //$NON-NLS-1$

	//private static final ConditionInfo POST_MUT_NOTSAT = new ConditionInfo("ext.framework.b.TestCondition", new String[] {"POST_MUT_NOTSAT", "true", "true", "false"}); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	//private static final ConditionInfo POST_NOTMUT_SAT = new ConditionInfo("ext.framework.b.TestCondition", new String[] {"POST_NOTMUT_SAT", "true", "false", "true"}); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	//private static final ConditionInfo POST_NOTMUT_NOTSAT = new ConditionInfo("ext.framework.b.TestCondition", new String[] {"POST_NOTMUT_NOTSAT", "true", "false", "false"}); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

	public static Test suite() {
		return new TestSuite(SecurityAdminUnitTests.class);
	}

	private Policy previousPolicy;
	private Equinox equinox;
	private ConditionalPermissionAdmin cpa;
	private PermissionAdmin pa;

	@Override
	protected void setUp() throws Exception {
		previousPolicy = Policy.getPolicy();
		final Permission allPermission = new AllPermission();
		final PermissionCollection allPermissions = new PermissionCollection() {
			private static final long serialVersionUID = 3258131349494708277L;

			// A simple PermissionCollection that only has AllPermission
		@Override
			public void add(Permission permission) {
				//no adding to this policy
			}

		@Override
			public boolean implies(Permission permission) {
				return true;
			}

		@Override
			public Enumeration elements() {
				return new Enumeration() {
					int cur = 0;

			@Override
					public boolean hasMoreElements() {
						return cur < 1;
					}

			@Override
					public Object nextElement() {
						if (cur == 0) {
							cur = 1;
							return allPermission;
						}
						throw new NoSuchElementException();
					}
				};
			}
		};

		Policy.setPolicy(new Policy() {

		@Override
			public PermissionCollection getPermissions(CodeSource codesource) {
				return allPermissions;
			}

		@Override
			public void refresh() {
				// nothing
			}

		});
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<String, Object>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_SECURITY, Constants.FRAMEWORK_SECURITY_OSGI);
		equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception on init()", e); //$NON-NLS-1$
		}
		cpa = equinox.getBundleContext().getService(equinox.getBundleContext().getServiceReference(ConditionalPermissionAdmin.class));
		pa = equinox.getBundleContext().getService(equinox.getBundleContext().getServiceReference(PermissionAdmin.class));
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		try {
			equinox.stop();
		} catch (BundleException e) {
			fail("Unexpected exception on stop()", e); //$NON-NLS-1$
		}
		if (System.getSecurityManager() != null)
			System.setSecurityManager(null);
		Policy.setPolicy(previousPolicy);
		super.tearDown();
	}

	public void testCreateDomain() {
		Bundle test = installTestBundle(TEST_BUNDLE);
		AccessControlContext acc = test.adapt(AccessControlContext.class);
		testPermission(acc, new AllPermission(), true);
	}

	public void testLocationPermission01() {
		Bundle test = installTestBundle(TEST_BUNDLE);
		AccessControlContext acc = test.adapt(AccessControlContext.class);
		pa.setPermissions(test.getLocation(), READONLY_INFOS);

		testPermission(acc, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new AllPermission(), false);

		pa.setPermissions(test.getLocation(), null);

		testPermission(acc, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new AllPermission(), true);
	}

	public void testLocationPermission02() {
		Bundle test = installTestBundle(TEST_BUNDLE);
		AccessControlContext acc = test.adapt(AccessControlContext.class);
		pa.setPermissions(test.getLocation(), READWRITE_INFOS);

		testPermission(acc, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new AllPermission(), false);

		pa.setPermissions(test.getLocation(), null);

		testPermission(acc, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new AllPermission(), true);
	}

	public void testLocationPermission03() {
		Bundle test = installTestBundle(TEST_BUNDLE);
		AccessControlContext acc = test.adapt(AccessControlContext.class);

		pa.setDefaultPermissions(READONLY_INFOS);
		pa.setPermissions(test.getLocation(), READWRITE_INFOS);
		ConditionalPermissionInfo condPermInfo = cpa.addConditionalPermissionInfo(ALLLOCATION_CONDS, SOCKET_INFOS);

		testPermission(acc, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new AllPermission(), false);
		testPermission(acc, new SocketPermission("localhost", "accept"), false); //$NON-NLS-1$ //$NON-NLS-2$

		pa.setPermissions(test.getLocation(), null);

		testPermission(acc, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new AllPermission(), false);
		testPermission(acc, new SocketPermission("localhost", "accept"), true); //$NON-NLS-1$ //$NON-NLS-2$

		condPermInfo.delete();
		testPermission(acc, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new AllPermission(), false);
		testPermission(acc, new SocketPermission("localhost", "accept"), false); //$NON-NLS-1$ //$NON-NLS-2$

		pa.setDefaultPermissions(null);
		testPermission(acc, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new AllPermission(), true);
		testPermission(acc, new SocketPermission("localhost", "accept"), true); //$NON-NLS-1$ //$NON-NLS-2$

	}

	public void testDefaultPermissions01() {
		Bundle test = installTestBundle(TEST_BUNDLE);
		AccessControlContext acc = test.adapt(AccessControlContext.class);
		pa.setDefaultPermissions(READONLY_INFOS);
		testPermission(acc, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new AllPermission(), false);

		pa.setDefaultPermissions(null);

		testPermission(acc, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new AllPermission(), true);
	}

	public void testDefaultPermissions02() {
		Bundle test = installTestBundle(TEST_BUNDLE);
		AccessControlContext acc = test.adapt(AccessControlContext.class);
		pa.setDefaultPermissions(READONLY_INFOS);

		pa.setPermissions(test.getLocation(), SOCKET_INFOS);

		testPermission(acc, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new AllPermission(), false);
		testPermission(acc, new SocketPermission("localhost", "accept"), true); //$NON-NLS-1$ //$NON-NLS-2$

		pa.setPermissions(test.getLocation(), null);

		testPermission(acc, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new AllPermission(), false);
		testPermission(acc, new SocketPermission("localhost", "accept"), false); //$NON-NLS-1$ //$NON-NLS-2$

		pa.setDefaultPermissions(null);

		testPermission(acc, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new AllPermission(), true);
	}

	public void testNotLocationCondition01() {
		Bundle test = installTestBundle(TEST_BUNDLE);
		AccessControlContext acc = test.adapt(AccessControlContext.class);

		ConditionalPermissionInfo condPermInfo = cpa.addConditionalPermissionInfo(getLocationConditions("xxx", true), SOCKET_INFOS); //$NON-NLS-1$
		testPermission(acc, new AllPermission(), false);
		testPermission(acc, new SocketPermission("localhost", "accept"), true); //$NON-NLS-1$ //$NON-NLS-2$

		condPermInfo.delete();
		testPermission(acc, new AllPermission(), true);
		testPermission(acc, new SocketPermission("localhost", "accept"), true); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testNotLocationCondition02() {
		Bundle test = installTestBundle(TEST_BUNDLE);
		AccessControlContext acc = test.adapt(AccessControlContext.class);

		ConditionalPermissionInfo condPermInfo = cpa.addConditionalPermissionInfo(getLocationConditions(test.getLocation(), true), SOCKET_INFOS);
		testPermission(acc, new AllPermission(), false);
		testPermission(acc, new SocketPermission("localhost", "accept"), false); //$NON-NLS-1$ //$NON-NLS-2$

		condPermInfo.delete();
		testPermission(acc, new AllPermission(), true);
		testPermission(acc, new SocketPermission("localhost", "accept"), true); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testMultipleLocationConditions01() {
		Bundle test = installTestBundle(TEST_BUNDLE);
		AccessControlContext acc = test.adapt(AccessControlContext.class);

		ConditionalPermissionInfo condPermInfo1 = cpa.addConditionalPermissionInfo(getLocationConditions("xxx", false), SOCKET_INFOS); //$NON-NLS-1$
		ConditionalPermissionInfo condPermInfo2 = cpa.addConditionalPermissionInfo(ALLLOCATION_CONDS, READONLY_INFOS);

		testPermission(acc, new SocketPermission("localhost", "accept"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$

		condPermInfo1.delete();
		testPermission(acc, new SocketPermission("localhost", "accept"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$

		condPermInfo2.delete();
		testPermission(acc, new SocketPermission("localhost", "accept"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testMultipleLocationConditions02() {
		Bundle test = installTestBundle(TEST_BUNDLE);
		AccessControlContext pd = test.adapt(AccessControlContext.class);

		ConditionalPermissionInfo condPermInfo1 = cpa.addConditionalPermissionInfo(getLocationConditions("xxx", false), SOCKET_INFOS); //$NON-NLS-1$
		ConditionalPermissionInfo condPermInfo2 = cpa.addConditionalPermissionInfo(ALLLOCATION_CONDS, READONLY_INFOS);
		ConditionalPermissionInfo condPermInfo3 = cpa.addConditionalPermissionInfo(getLocationConditions(test.getLocation(), false), RUNTIME_INFOS);

		testPermission(pd, new SocketPermission("localhost", "accept"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new RuntimePermission("exitVM", null), true); //$NON-NLS-1$
		testPermission(pd, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$

		condPermInfo1.delete();
		testPermission(pd, new SocketPermission("localhost", "accept"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new RuntimePermission("exitVM", null), true); //$NON-NLS-1$
		testPermission(pd, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$

		condPermInfo2.delete();
		testPermission(pd, new SocketPermission("localhost", "accept"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new RuntimePermission("exitVM", null), true); //$NON-NLS-1$
		testPermission(pd, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$

		condPermInfo3.delete();
		testPermission(pd, new SocketPermission("localhost", "accept"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new RuntimePermission("exitVM", null), true); //$NON-NLS-1$
		testPermission(pd, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(pd, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testUpdate01() {
		ConditionalPermissionUpdate update = cpa.newConditionalPermissionUpdate();
		List rows = update.getConditionalPermissionInfos();
		assertTrue("table is not empty", rows.isEmpty()); //$NON-NLS-1$
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$
	}

	public void testUpdate02() {
		ConditionalPermissionUpdate update = cpa.newConditionalPermissionUpdate();
		List rows = update.getConditionalPermissionInfos();
		ConditionalPermissionInfo info = cpa.newConditionalPermissionInfo(null, ALLLOCATION_CONDS, READONLY_INFOS, ConditionalPermissionInfo.ALLOW);
		rows.add(info);
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$

		Bundle test = installTestBundle(TEST_BUNDLE);
		AccessControlContext acc = test.adapt(AccessControlContext.class);
		testPermission(acc, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new AllPermission(), false);

		update = cpa.newConditionalPermissionUpdate();
		rows = update.getConditionalPermissionInfos();
		rows.clear();
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$
		testPermission(acc, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new AllPermission(), true);
	}

	public void testUpdate03() {
		ConditionalPermissionUpdate update = cpa.newConditionalPermissionUpdate();
		List rows = update.getConditionalPermissionInfos();
		ConditionalPermissionInfo info1 = cpa.newConditionalPermissionInfo(null, ALLLOCATION_CONDS, READWRITE_INFOS, ConditionalPermissionInfo.DENY);
		ConditionalPermissionInfo info2 = cpa.newConditionalPermissionInfo(null, ALLLOCATION_CONDS, READONLY_INFOS, ConditionalPermissionInfo.ALLOW);
		rows.add(info1);
		rows.add(info2);
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$

		Bundle test = installTestBundle(TEST_BUNDLE);
		AccessControlContext acc = test.adapt(AccessControlContext.class);
		testPermission(acc, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new AllPermission(), false);

		update = cpa.newConditionalPermissionUpdate();
		rows = update.getConditionalPermissionInfos();
		rows.remove(0);
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$
		testPermission(acc, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new AllPermission(), false);

		update = cpa.newConditionalPermissionUpdate();
		rows = update.getConditionalPermissionInfos();
		rows.remove(0);
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$
		testPermission(acc, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new AllPermission(), true);
	}

	public void testUpdate04() {
		ConditionalPermissionUpdate update = cpa.newConditionalPermissionUpdate();
		List rows = update.getConditionalPermissionInfos();
		ConditionalPermissionInfo info1 = cpa.newConditionalPermissionInfo(null, ALLLOCATION_CONDS, READWRITE_INFOS, ConditionalPermissionInfo.DENY);
		ConditionalPermissionInfo info2 = cpa.newConditionalPermissionInfo(null, ALLLOCATION_CONDS, READONLY_INFOS, ConditionalPermissionInfo.ALLOW);
		rows.add(info1);
		rows.add(info2);
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$

		ConditionalPermissionUpdate update1 = cpa.newConditionalPermissionUpdate();
		List rows1 = update1.getConditionalPermissionInfos();
		rows1.remove(0);

		ConditionalPermissionUpdate update2 = cpa.newConditionalPermissionUpdate();
		List rows2 = update2.getConditionalPermissionInfos();
		rows2.remove(0);
		assertTrue("failed to commit", update2.commit()); //$NON-NLS-1$

		Bundle test = installTestBundle(TEST_BUNDLE);
		AccessControlContext acc = test.adapt(AccessControlContext.class);
		testPermission(acc, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new AllPermission(), false);

		assertFalse("succeeded commit", update1.commit()); //$NON-NLS-1$

		update = cpa.newConditionalPermissionUpdate();
		rows = update.getConditionalPermissionInfos();
		rows.remove(0);

		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$
		testPermission(acc, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new AllPermission(), true);
	}

	public void testSecurityManager01() {
		ConditionalPermissionUpdate update = cpa.newConditionalPermissionUpdate();
		List rows = update.getConditionalPermissionInfos();
		ConditionalPermissionInfo info = cpa.newConditionalPermissionInfo(null, ALLLOCATION_CONDS, READONLY_INFOS, ConditionalPermissionInfo.ALLOW);
		rows.add(info);
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$

		Bundle test = installTestBundle(TEST_BUNDLE);
		ProtectionDomain pd = test.adapt(ProtectionDomain.class);
		ProtectionDomain[] pds = new ProtectionDomain[] {pd};
		testSMPermission(pds, new FilePermission("test", "write"), false); //$NON-NLS-1$ //$NON-NLS-2$
		testSMPermission(pds, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testSMPermission(pds, new AllPermission(), false);

		update = cpa.newConditionalPermissionUpdate();
		rows = update.getConditionalPermissionInfos();
		rows.clear();
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$
		testSMPermission(pds, new FilePermission("test", "write"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testSMPermission(pds, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testSMPermission(pds, new AllPermission(), true);
	}

	public void testPostponedConditions01() {
		installConditionBundle();
		TestCondition.clearConditions();
		Bundle test1 = installTestBundle(TEST_BUNDLE);
		Bundle test2 = installTestBundle(TEST2_BUNDLE);
		ProtectionDomain pd1 = test1.adapt(ProtectionDomain.class);
		ProtectionDomain pd2 = test2.adapt(ProtectionDomain.class);
		ProtectionDomain[] pds = new ProtectionDomain[] {pd1, pd2};

		ConditionalPermissionUpdate update = cpa.newConditionalPermissionUpdate();
		List rows = update.getConditionalPermissionInfos();
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {POST_MUT_SAT}, READONLY_INFOS, ConditionalPermissionInfo.DENY));
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {POST_MUT_UNSAT}, READONLY_INFOS, ConditionalPermissionInfo.ALLOW));
		rows.add(cpa.newConditionalPermissionInfo(null, ALLLOCATION_CONDS, READONLY_INFOS, ConditionalPermissionInfo.DENY));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$);

		testSMPermission(pds, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$

		TestCondition tc1sat = TestCondition.getTestCondition("POST_MUT_SAT_" + test1.getBundleId()); //$NON-NLS-1$
		TestCondition tc2sat = TestCondition.getTestCondition("POST_MUT_SAT_" + test2.getBundleId()); //$NON-NLS-1$
		TestCondition tc1unsat = TestCondition.getTestCondition("POST_MUT_UNSAT_" + test1.getBundleId()); //$NON-NLS-1$
		TestCondition tc2unsat = TestCondition.getTestCondition("POST_MUT_UNSAT_" + test2.getBundleId()); //$NON-NLS-1$

		assertNotNull("tc1sat", tc1sat); //$NON-NLS-1$
		assertNotNull("tc2sat", tc2sat); //$NON-NLS-1$
		assertNotNull("tc1unsat", tc1unsat); //$NON-NLS-1$
		assertNotNull("tc2unsat", tc2unsat); //$NON-NLS-1$

		tc1sat.setSatisfied(false);
		tc2sat.setSatisfied(false);
		tc1unsat.setSatisfied(true);
		tc2unsat.setSatisfied(true);
		testSMPermission(pds, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$

		tc1sat.setSatisfied(true);
		tc2sat.setSatisfied(true);
		update = cpa.newConditionalPermissionUpdate();
		rows = update.getConditionalPermissionInfos();
		rows.remove(0);
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$);
		testSMPermission(pds, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$

		tc1unsat.setSatisfied(false);
		tc2unsat.setSatisfied(false);
		testSMPermission(pds, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$

		update = cpa.newConditionalPermissionUpdate();
		rows = update.getConditionalPermissionInfos();
		rows.remove(0);
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$);
		testSMPermission(pds, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testPostponedConditions02() {
		installConditionBundle();
		TestCondition.clearConditions();

		Bundle test1 = installTestBundle(TEST_BUNDLE);
		Bundle test2 = installTestBundle(TEST2_BUNDLE);
		ProtectionDomain pd1 = test1.adapt(ProtectionDomain.class);
		ProtectionDomain pd2 = test2.adapt(ProtectionDomain.class);
		ProtectionDomain[] pds = new ProtectionDomain[] {pd1, pd2};

		ConditionalPermissionUpdate update = cpa.newConditionalPermissionUpdate();
		List rows = update.getConditionalPermissionInfos();
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {POST_MUT_SAT}, READONLY_INFOS, ConditionalPermissionInfo.DENY));
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {POST_MUT_SAT}, READONLY_INFOS, ConditionalPermissionInfo.DENY));
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {POST_MUT_UNSAT}, READONLY_INFOS, ConditionalPermissionInfo.DENY));
		rows.add(cpa.newConditionalPermissionInfo(null, ALLLOCATION_CONDS, READONLY_INFOS, ConditionalPermissionInfo.DENY));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$);

		testSMPermission(pds, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$

		TestCondition tc1sat = TestCondition.getTestCondition("POST_MUT_SAT_" + test1.getBundleId()); //$NON-NLS-1$
		TestCondition tc2sat = TestCondition.getTestCondition("POST_MUT_SAT_" + test2.getBundleId()); //$NON-NLS-1$
		TestCondition tc1unsat = TestCondition.getTestCondition("POST_MUT_UNSAT_" + test1.getBundleId()); //$NON-NLS-1$
		TestCondition tc2unsat = TestCondition.getTestCondition("POST_MUT_UNSAT_" + test2.getBundleId()); //$NON-NLS-1$

		// Note that we need to avoid an ordering assumption on the order in which
		// ProtectionDomains are processed by the AccessControlContext (bug 269917)
		// Just make sure both tc1 and tc2 are not non-null at the same time.
		assertTrue("tc1sat and tc2sat are either both null or both non-null", (tc1sat == null) ^ (tc2sat == null)); //$NON-NLS-1$
		assertTrue("tc1unsat and tc2unsat are either both null or both non-null", (tc1unsat == null) ^ (tc2unsat == null)); //$NON-NLS-1$

		TestCondition modifySat = tc1sat != null ? tc1sat : tc2sat;
		TestCondition modifyUnsat = tc1unsat != null ? tc1unsat : tc2unsat;
		modifySat.setSatisfied(false);
		modifyUnsat.setSatisfied(true);
		testSMPermission(pds, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testPostponedConditions03() {
		installConditionBundle();
		TestCondition.clearConditions();

		Bundle test1 = installTestBundle(TEST_BUNDLE);
		Bundle test2 = installTestBundle(TEST2_BUNDLE);
		ProtectionDomain pd1 = test1.adapt(ProtectionDomain.class);
		ProtectionDomain pd2 = test2.adapt(ProtectionDomain.class);
		ProtectionDomain[] pds = new ProtectionDomain[] {pd1, pd2};

		ConditionalPermissionUpdate update = cpa.newConditionalPermissionUpdate();
		List rows = update.getConditionalPermissionInfos();
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {POST_MUT_SAT}, READONLY_INFOS, ConditionalPermissionInfo.ALLOW));
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {POST_MUT_SAT}, READONLY_INFOS, ConditionalPermissionInfo.ALLOW));
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {POST_MUT_UNSAT}, READONLY_INFOS, ConditionalPermissionInfo.ALLOW));
		rows.add(cpa.newConditionalPermissionInfo(null, ALLLOCATION_CONDS, READONLY_INFOS, ConditionalPermissionInfo.ALLOW));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$);

		testSMPermission(pds, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$

		TestCondition tc1sat = TestCondition.getTestCondition("POST_MUT_SAT_" + test1.getBundleId()); //$NON-NLS-1$
		TestCondition tc2sat = TestCondition.getTestCondition("POST_MUT_SAT_" + test2.getBundleId()); //$NON-NLS-1$
		TestCondition tc1unsat = TestCondition.getTestCondition("POST_MUT_UNSAT_" + test1.getBundleId()); //$NON-NLS-1$
		TestCondition tc2unsat = TestCondition.getTestCondition("POST_MUT_UNSAT_" + test2.getBundleId()); //$NON-NLS-1$

		assertNotNull("tc1sat", tc1sat); //$NON-NLS-1$
		assertNotNull("tc2sat", tc2sat); //$NON-NLS-1$
		assertNotNull("tc1unsat", tc1unsat); //$NON-NLS-1$
		assertNotNull("tc2unsat", tc2unsat); //$NON-NLS-1$

		tc1sat.setSatisfied(false);
		tc2sat.setSatisfied(false);
		tc1unsat.setSatisfied(true);
		tc2unsat.setSatisfied(true);
		testSMPermission(pds, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testPostponedConditions04() {
		installConditionBundle();
		TestCondition.clearConditions();

		Bundle test1 = installTestBundle(TEST_BUNDLE);
		Bundle test2 = installTestBundle(TEST2_BUNDLE);
		ProtectionDomain pd1 = test1.adapt(ProtectionDomain.class);
		ProtectionDomain pd2 = test2.adapt(ProtectionDomain.class);
		ProtectionDomain[] pds = new ProtectionDomain[] {pd1, pd2};

		ConditionalPermissionUpdate update = cpa.newConditionalPermissionUpdate();
		List rows = update.getConditionalPermissionInfos();
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {POST_MUT_SAT}, READONLY_INFOS, ConditionalPermissionInfo.DENY));
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {POST_MUT_SAT}, READONLY_INFOS, ConditionalPermissionInfo.DENY));
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {POST_MUT_UNSAT}, READONLY_INFOS, ConditionalPermissionInfo.DENY));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$);

		testSMPermission(pds, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$

		TestCondition tc1sat = TestCondition.getTestCondition("POST_MUT_SAT_" + test1.getBundleId()); //$NON-NLS-1$
		TestCondition tc2sat = TestCondition.getTestCondition("POST_MUT_SAT_" + test2.getBundleId()); //$NON-NLS-1$
		TestCondition tc1unsat = TestCondition.getTestCondition("POST_MUT_UNSAT_" + test1.getBundleId()); //$NON-NLS-1$
		TestCondition tc2unsat = TestCondition.getTestCondition("POST_MUT_UNSAT_" + test2.getBundleId()); //$NON-NLS-1$

		// Note that we need to avoid an ordering assumption on the order in which
		// ProtectionDomains are processed by the AccessControlContext (bug 269917)
		// Just make sure both tc1 and tc2 are not non-null at the same time.
		assertTrue("tc1sat and tc2sat are either both null or both non-null", (tc1sat == null) ^ (tc2sat == null)); //$NON-NLS-1$
		assertTrue("tc1unsat and tc2unsat are either both null or both non-null", (tc1unsat == null) ^ (tc2unsat == null)); //$NON-NLS-1$

		TestCondition modifySat = tc1sat != null ? tc1sat : tc2sat;
		TestCondition modifyUnsat = tc1unsat != null ? tc1unsat : tc2unsat;
		modifySat.setSatisfied(false);
		modifyUnsat.setSatisfied(true);
		testSMPermission(pds, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testPostponedConditions05() {
		installConditionBundle();
		TestCondition.clearConditions();

		Bundle test1 = installTestBundle(TEST_BUNDLE);
		Bundle test2 = installTestBundle(TEST2_BUNDLE);
		ProtectionDomain pd1 = test1.adapt(ProtectionDomain.class);
		ProtectionDomain pd2 = test2.adapt(ProtectionDomain.class);
		ProtectionDomain[] pds = new ProtectionDomain[] {pd1, pd2};

		ConditionalPermissionUpdate update = cpa.newConditionalPermissionUpdate();
		List rows = update.getConditionalPermissionInfos();
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {POST_MUT_SAT}, READONLY_INFOS, ConditionalPermissionInfo.DENY));
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {POST_MUT_UNSAT}, READONLY_INFOS, ConditionalPermissionInfo.ALLOW));
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {POST_MUT_SAT}, READONLY_INFOS, ConditionalPermissionInfo.ALLOW));
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {POST_MUT_UNSAT}, READONLY_INFOS, ConditionalPermissionInfo.ALLOW));
		rows.add(cpa.newConditionalPermissionInfo(null, ALLLOCATION_CONDS, READONLY_INFOS, ConditionalPermissionInfo.ALLOW));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$);

		testSMPermission(pds, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$

		TestCondition tc1sat = TestCondition.getTestCondition("POST_MUT_SAT_" + test1.getBundleId()); //$NON-NLS-1$
		TestCondition tc2sat = TestCondition.getTestCondition("POST_MUT_SAT_" + test2.getBundleId()); //$NON-NLS-1$
		TestCondition tc1unsat = TestCondition.getTestCondition("POST_MUT_UNSAT_" + test1.getBundleId()); //$NON-NLS-1$
		TestCondition tc2unsat = TestCondition.getTestCondition("POST_MUT_UNSAT_" + test2.getBundleId()); //$NON-NLS-1$

		assertNotNull("tc1sat", tc1sat); //$NON-NLS-1$
		assertNotNull("tc2sat", tc2sat); //$NON-NLS-1$
		assertNotNull("tc1unsat", tc1unsat); //$NON-NLS-1$
		assertNotNull("tc2unsat", tc2unsat); //$NON-NLS-1$

		tc1sat.setSatisfied(false);
		tc2sat.setSatisfied(false);
		testSMPermission(pds, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testMutableConditions() {
		installConditionBundle();
		TestCondition.clearConditions();

		Bundle test1 = installTestBundle(TEST_BUNDLE);
		ProtectionDomain pd1 = test1.adapt(ProtectionDomain.class);
		ProtectionDomain[] pds = new ProtectionDomain[] {pd1};

		ConditionalPermissionUpdate update = cpa.newConditionalPermissionUpdate();
		List<ConditionalPermissionInfo> rows = update.getConditionalPermissionInfos();
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {MUT_SAT}, READONLY_INFOS, ConditionalPermissionInfo.DENY));
		rows.add(cpa.newConditionalPermissionInfo(null, ALLLOCATION_CONDS, READONLY_INFOS, ConditionalPermissionInfo.ALLOW));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$);

		testSMPermission(pds, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$

		TestCondition tc1sat = TestCondition.getTestCondition("MUT_SAT_" + test1.getBundleId()); //$NON-NLS-1$

		assertNotNull("tc1sat", tc1sat); //$NON-NLS-1$

		tc1sat.setSatisfied(false);
		testSMPermission(pds, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$

		tc1sat.setSatisfied(true);
		update = cpa.newConditionalPermissionUpdate();
		rows = update.getConditionalPermissionInfos();
		rows.clear();
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {NOT_MUT_SAT, MUT_SAT}, READONLY_INFOS, ConditionalPermissionInfo.DENY));
		rows.add(cpa.newConditionalPermissionInfo(null, ALLLOCATION_CONDS, READONLY_INFOS, ConditionalPermissionInfo.ALLOW));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$);

		testSMPermission(pds, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$
		// test again to make sure we get the same result
		testSMPermission(pds, new FilePermission("test", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$
		// test with different file name
		testSMPermission(pds, new FilePermission("test2", "read"), false); //$NON-NLS-1$ //$NON-NLS-2$

		TestCondition tc2sat = TestCondition.getTestCondition("NOT_MUT_SAT_" + test1.getBundleId()); //$NON-NLS-1$
		assertNotNull("tc2sat", tc2sat); //$NON-NLS-1$

	}

	public void testAccessControlContext01() {
		// test single row with signer condition
		ConditionalPermissionUpdate update = cpa.newConditionalPermissionUpdate();
		List rows = update.getConditionalPermissionInfos();
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {SIGNER_CONDITION1}, READONLY_INFOS, ConditionalPermissionInfo.ALLOW));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$

		AccessControlContext acc = cpa.getAccessControlContext(new String[] {"cn=t1,c=FR;cn=test1,c=US"}); //$NON-NLS-1$
		try {
			acc.checkPermission(new FilePermission("test", "write")); //$NON-NLS-1$ //$NON-NLS-2$
			fail("expecting AccessControlExcetpion"); //$NON-NLS-1$
		} catch (AccessControlException e) {
			// expected
		}
		try {
			acc.checkPermission(new FilePermission("test", "read")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (AccessControlException e) {
			fail("Unexpected AccessControlExcetpion", e); //$NON-NLS-1$
		}
	}

	public void testAccessControlContext01a() {
		// test single row with signer condition
		ConditionalPermissionUpdate update = cpa.newConditionalPermissionUpdate();
		List rows = update.getConditionalPermissionInfos();
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {SIGNER_CONDITION1}, READONLY_INFOS, ConditionalPermissionInfo.ALLOW));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$

		AccessControlContext acc = cpa.getAccessControlContext(new String[] {"cn=test1,c=US"}); //$NON-NLS-1$
		try {
			acc.checkPermission(new FilePermission("test", "write")); //$NON-NLS-1$ //$NON-NLS-2$
			fail("expecting AccessControlExcetpion"); //$NON-NLS-1$
		} catch (AccessControlException e) {
			// expected
		}
		try {
			acc.checkPermission(new FilePermission("test", "read")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (AccessControlException e) {
			fail("Unexpected AccessControlExcetpion", e); //$NON-NLS-1$
		}
	}

	public void testAccessControlContext02() {
		// test with DENY row
		ConditionalPermissionUpdate update = cpa.newConditionalPermissionUpdate();
		List rows = update.getConditionalPermissionInfos();
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {SIGNER_CONDITION1}, READONLY_INFOS, ConditionalPermissionInfo.ALLOW));
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {SIGNER_CONDITION1}, READWRITE_INFOS, ConditionalPermissionInfo.DENY));
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {SIGNER_CONDITION1}, READWRITE_INFOS, ConditionalPermissionInfo.ALLOW));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$

		AccessControlContext acc = cpa.getAccessControlContext(new String[] {"cn=t1,c=FR;cn=test1,c=US"}); //$NON-NLS-1$
		try {
			acc.checkPermission(new FilePermission("test", "write")); //$NON-NLS-1$ //$NON-NLS-2$
			fail("expecting AccessControlExcetpion"); //$NON-NLS-1$
		} catch (AccessControlException e) {
			// expected
		}
		try {
			acc.checkPermission(new FilePermission("test", "read")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (AccessControlException e) {
			fail("Unexpected AccessControlExcetpion", e); //$NON-NLS-1$
		}
	}

	public void testAccessControlContext03() {
		// test multiple signer conditions
		ConditionalPermissionUpdate update = cpa.newConditionalPermissionUpdate();
		List rows = update.getConditionalPermissionInfos();
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {SIGNER_CONDITION1}, READONLY_INFOS, ConditionalPermissionInfo.ALLOW));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$

		AccessControlContext acc = cpa.getAccessControlContext(new String[] {"cn=t1,c=FR;cn=test2,c=US"}); //$NON-NLS-1$
		try {
			acc.checkPermission(new FilePermission("test", "write")); //$NON-NLS-1$ //$NON-NLS-2$
			fail("expecting AccessControlExcetpion"); //$NON-NLS-1$
		} catch (AccessControlException e) {
			// expected
		}
		try {
			acc.checkPermission(new FilePermission("test", "read")); //$NON-NLS-1$ //$NON-NLS-2$
			fail("expecting AccessControlExcetpion"); //$NON-NLS-1$
		} catch (AccessControlException e) {
			// expected
		}

		update = cpa.newConditionalPermissionUpdate();
		rows = update.getConditionalPermissionInfos();
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {SIGNER_CONDITION2}, READONLY_INFOS, ConditionalPermissionInfo.ALLOW));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$
		acc = cpa.getAccessControlContext(new String[] {"cn=t1,c=FR;cn=test2,c=US"}); //$NON-NLS-1$
		try {
			acc.checkPermission(new FilePermission("test", "write")); //$NON-NLS-1$ //$NON-NLS-2$
			fail("expecting AccessControlExcetpion"); //$NON-NLS-1$
		} catch (AccessControlException e) {
			// expected
		}
		try {
			acc.checkPermission(new FilePermission("test", "read")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (AccessControlException e) {
			fail("Unexpected AccessControlExcetpion", e); //$NON-NLS-1$
		}
	}

	public void testAccessControlContext04() {
		// test multiple signer conditions
		ConditionalPermissionUpdate update = cpa.newConditionalPermissionUpdate();
		List rows = update.getConditionalPermissionInfos();
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {SIGNER_CONDITION1, SIGNER_CONDITION2}, READONLY_INFOS, ConditionalPermissionInfo.ALLOW));
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {SIGNER_CONDITION1}, READWRITE_INFOS, ConditionalPermissionInfo.ALLOW));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$

		AccessControlContext acc = cpa.getAccessControlContext(new String[] {"cn=t1,c=FR;cn=test2,c=US"}); //$NON-NLS-1$
		try {
			acc.checkPermission(new FilePermission("test", "write")); //$NON-NLS-1$ //$NON-NLS-2$
			fail("expecting AccessControlExcetpion"); //$NON-NLS-1$
		} catch (AccessControlException e) {
			// expected
		}
		try {
			acc.checkPermission(new FilePermission("test", "read")); //$NON-NLS-1$ //$NON-NLS-2$
			fail("expecting AccessControlExcetpion"); //$NON-NLS-1$
		} catch (AccessControlException e) {
			// expected
		}

		acc = cpa.getAccessControlContext(new String[] {"cn=t1,c=FR;cn=test1,c=US", "cn=t1,c=FR;cn=test2,c=US"}); //$NON-NLS-1$ //$NON-NLS-2$
		try {
			acc.checkPermission(new FilePermission("test", "write")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (AccessControlException e) {
			fail("Unexpected AccessControlExcetpion", e); //$NON-NLS-1$
		}
		try {
			acc.checkPermission(new FilePermission("test", "read")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (AccessControlException e) {
			fail("Unexpected AccessControlExcetpion", e); //$NON-NLS-1$
		}
	}

	public void testAccessControlContext05() {
		// test with empty rows
		AccessControlContext acc = cpa.getAccessControlContext(new String[] {"cn=t1,c=FR;cn=test2,c=US"}); //$NON-NLS-1$
		try {
			acc.checkPermission(new FilePermission("test", "write")); //$NON-NLS-1$ //$NON-NLS-2$
			acc.checkPermission(new FilePermission("test", "read")); //$NON-NLS-1$ //$NON-NLS-2$
			acc.checkPermission(new AllPermission());
		} catch (AccessControlException e) {
			fail("Unexpected AccessControlExcetpion", e); //$NON-NLS-1$
		}
		// set the default permissions
		pa.setDefaultPermissions(READWRITE_INFOS);
		acc = cpa.getAccessControlContext(new String[] {"cn=t1,c=FR;cn=test2,c=US"}); //$NON-NLS-1$
		try {
			acc.checkPermission(new FilePermission("test", "write")); //$NON-NLS-1$ //$NON-NLS-2$
			acc.checkPermission(new FilePermission("test", "read")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (AccessControlException e) {
			fail("Unexpected AccessControlExcetpion", e); //$NON-NLS-1$
		}
		try {
			acc.checkPermission(new AllPermission());
			fail("expecting AccessControlExcetpion"); //$NON-NLS-1$
		} catch (AccessControlException e) {
			// expected
		}
	}

	public void testAccessControlContext06() {
		// test with empty condition rows
		ConditionalPermissionUpdate update = cpa.newConditionalPermissionUpdate();
		List rows = update.getConditionalPermissionInfos();
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {}, READONLY_INFOS, ConditionalPermissionInfo.ALLOW));
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {SIGNER_CONDITION1}, READWRITE_INFOS, ConditionalPermissionInfo.ALLOW));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$

		AccessControlContext acc = cpa.getAccessControlContext(new String[] {"cn=t1,c=FR;cn=test2,c=US"}); //$NON-NLS-1$
		try {
			acc.checkPermission(new FilePermission("test", "read")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (AccessControlException e) {
			fail("Unexpected AccessControlExcetpion", e); //$NON-NLS-1$
		}
		try {
			acc.checkPermission(new FilePermission("test", "write")); //$NON-NLS-1$ //$NON-NLS-2$
			fail("expecting AccessControlExcetpion"); //$NON-NLS-1$
		} catch (AccessControlException e) {
			// expected
		}
	}

	public void testAccessControlContext07() {
		// test ! signer condition
		ConditionalPermissionUpdate update = cpa.newConditionalPermissionUpdate();
		List rows = update.getConditionalPermissionInfos();
		rows.add(cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {NOT_SIGNER_CONDITION1}, READONLY_INFOS, ConditionalPermissionInfo.ALLOW));
		assertTrue("failed to commit", update.commit()); //$NON-NLS-1$
		AccessControlContext acc = cpa.getAccessControlContext(new String[] {"cn=t1,c=FR;cn=test1,c=US"}); //$NON-NLS-1$
		try {
			acc.checkPermission(new FilePermission("test", "write")); //$NON-NLS-1$ //$NON-NLS-2$
			fail("expecting AccessControlExcetpion"); //$NON-NLS-1$
		} catch (AccessControlException e) {
			// expected
		}
		try {
			acc.checkPermission(new FilePermission("test", "read")); //$NON-NLS-1$ //$NON-NLS-2$
			fail("expecting AccessControlExcetpion"); //$NON-NLS-1$
		} catch (AccessControlException e) {
			// expected
		}

		acc = cpa.getAccessControlContext(new String[] {"cn=t1,c=FR;cn=test2,c=US"}); //$NON-NLS-1$
		try {
			acc.checkPermission(new FilePermission("test", "write")); //$NON-NLS-1$ //$NON-NLS-2$
			fail("expecting AccessControlExcetpion"); //$NON-NLS-1$
		} catch (AccessControlException e) {
			// expected
		}
		try {
			acc.checkPermission(new FilePermission("test", "read")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (AccessControlException e) {
			fail("Unexpected AccessControlExcetpion", e); //$NON-NLS-1$
		}
	}

	public void testEncodingInfos01() throws Exception {
		String info1 = "ALLOW { [Test1] (Type1 \"name1\" \"action1\") } \"name1\""; //$NON-NLS-1$
		String info2 = "ALLOW { [Test2] (Type2 \"name2\" \"action2\") } \"name2\""; //$NON-NLS-1$

		ConditionalPermissionUpdate update = cpa.newConditionalPermissionUpdate();
		List updateInfos = update.getConditionalPermissionInfos();
		updateInfos.add(cpa.newConditionalPermissionInfo(info1));
		updateInfos.add(cpa.newConditionalPermissionInfo(info2));
		assertTrue("Failed commit", update.commit()); //$NON-NLS-1$

		equinox.stop();
		equinox.waitForStop(10000);
		equinox.init();
		cpa = equinox.getBundleContext().getService(equinox.getBundleContext().getServiceReference(ConditionalPermissionAdmin.class));
		pa = equinox.getBundleContext().getService(equinox.getBundleContext().getServiceReference(PermissionAdmin.class));

		String info3 = "deny { [Test3] (Type3 \"name3\" \"action3\") } \"name3\""; //$NON-NLS-1$

		ArrayList infos = new ArrayList();
		for (Enumeration eInfos = cpa.getConditionalPermissionInfos(); eInfos.hasMoreElements();)
			infos.add(eInfos.nextElement());
		assertEquals("Wrong number of infos", 2, infos.size()); //$NON-NLS-1$
		assertTrue("Missing info1", infos.contains(cpa.newConditionalPermissionInfo(info1))); //$NON-NLS-1$
		assertTrue("Missing info2", infos.contains(cpa.newConditionalPermissionInfo(info2))); //$NON-NLS-1$
		assertEquals("Wrong index of info1", 0, infos.indexOf(cpa.newConditionalPermissionInfo(info1))); //$NON-NLS-1$
		assertEquals("Wrong index of info2", 1, infos.indexOf(cpa.newConditionalPermissionInfo(info2))); //$NON-NLS-1$

		update = cpa.newConditionalPermissionUpdate();
		updateInfos = update.getConditionalPermissionInfos();
		assertTrue("Info lists are not equal", updateInfos.equals(infos)); //$NON-NLS-1$
		updateInfos.add(cpa.newConditionalPermissionInfo(info3));
		assertTrue("Failed commit", update.commit()); //$NON-NLS-1$

		infos = new ArrayList();
		for (Enumeration eInfos = cpa.getConditionalPermissionInfos(); eInfos.hasMoreElements();)
			infos.add(eInfos.nextElement());
		assertTrue("Info lists are not equal", updateInfos.equals(infos)); //$NON-NLS-1$
	}

	public void testEncodingInfos02() {

		ConditionInfo cond1 = new ConditionInfo("Test1", new String[] {"arg1", "arg2"}); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		ConditionInfo cond2 = new ConditionInfo("Test1", new String[] {"arg1", "arg2", "arg3"}); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
		ConditionInfo cond3 = new ConditionInfo("Test1", new String[] {"test } test", "} test"}); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

		PermissionInfo perm1 = new PermissionInfo("Type1", "name1", "action1"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		PermissionInfo perm2 = new PermissionInfo("Type1", "}", "test }"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

		// good info; mix case decision
		ConditionalPermissionInfo testInfo1 = cpa.newConditionalPermissionInfo("name1", new ConditionInfo[] {cond1}, new PermissionInfo[] {perm1}, "allow"); //$NON-NLS-1$ //$NON-NLS-2$
		ConditionalPermissionInfo testInfo2 = checkGoodInfo("AlLoW { [Test1 \"arg1\" \"arg2\"] (Type1 \"name1\" \"action1\") } \"name1\""); //$NON-NLS-1$
		checkInfos(testInfo1, testInfo2);
		testInfo1 = cpa.newConditionalPermissionInfo("name1", new ConditionInfo[] {cond2}, new PermissionInfo[] {perm1}, "deny"); //$NON-NLS-1$ //$NON-NLS-2$
		testInfo2 = checkGoodInfo("dEnY { [Test1 \"arg1\" \"arg2\" \"arg3\"] (Type1 \"name1\" \"action1\") } \"name1\""); //$NON-NLS-1$
		checkInfos(testInfo1, testInfo2);

		// good info; no conditions
		testInfo1 = cpa.newConditionalPermissionInfo("name1", null, new PermissionInfo[] {perm1}, "deny"); //$NON-NLS-1$ //$NON-NLS-2$
		testInfo2 = checkGoodInfo("dEnY { (Type1 \"name1\" \"action1\") } \"name1\""); //$NON-NLS-1$
		checkInfos(testInfo1, testInfo2);

		// good info; no name
		testInfo1 = cpa.newConditionalPermissionInfo(null, new ConditionInfo[] {cond1}, new PermissionInfo[] {perm1}, "allow"); //$NON-NLS-1$
		testInfo2 = checkGoodInfo("allow { [Test1 \"arg1\" \"arg2\"] (Type1 \"name1\" \"action1\") }"); //$NON-NLS-1$
		checkInfos(testInfo1, testInfo2);

		// good info; empty name
		testInfo1 = cpa.newConditionalPermissionInfo("", new ConditionInfo[] {cond1}, new PermissionInfo[] {perm1}, "allow"); //$NON-NLS-1$ //$NON-NLS-2$
		testInfo2 = checkGoodInfo("allow { [Test1 \"arg1\" \"arg2\"] (Type1 \"name1\" \"action1\") } \"\""); //$NON-NLS-1$
		checkInfos(testInfo1, testInfo2);

		// good info; no white space
		testInfo1 = cpa.newConditionalPermissionInfo("name1", new ConditionInfo[] {cond1}, new PermissionInfo[] {perm1}, "allow"); //$NON-NLS-1$ //$NON-NLS-2$
		testInfo2 = checkGoodInfo("allow{[Test1 \"arg1\" \"arg2\"](Type1 \"name1\" \"action1\")}\"name1\""); //$NON-NLS-1$
		checkInfos(testInfo1, testInfo2);

		// good info; '}' in quoted value
		testInfo1 = cpa.newConditionalPermissionInfo("name", new ConditionInfo[] {cond3}, new PermissionInfo[] {perm2}, "allow"); //$NON-NLS-1$ //$NON-NLS-2$
		testInfo2 = checkGoodInfo("allow { [Test1 \"test } test\" \"} test\"] (Type1 \"}\" \"test }\") } \"name\""); //$NON-NLS-1$
		checkInfos(testInfo1, testInfo2);

		// good info; '}' in quoted value
		testInfo1 = cpa.newConditionalPermissionInfo("na } me", new ConditionInfo[] {cond3}, new PermissionInfo[] {perm2}, "allow"); //$NON-NLS-1$ //$NON-NLS-2$
		testInfo2 = checkGoodInfo("allow { [Test1 \"test } test\" \"} test\"] (Type1 \"}\" \"test }\") } \"na } me\""); //$NON-NLS-1$
		checkInfos(testInfo1, testInfo2);

		// bad decision test
		checkBadInfo("invalid { [Test1] (Type1 \"name1\" \"action1\") } \"name1\""); //$NON-NLS-1$
		// bad condition; missing type
		checkBadInfo("allow { [] (Type1 \"name1\" \"action1\") } \"name1\""); //$NON-NLS-1$
		checkBadInfo("deny { [\"arg1\"] (Type1 \"name1\" \"action1\") } \"name1\""); //$NON-NLS-1$
		// bad permission (none)
		checkBadInfo("ALLOW { [Test1 \"arg1\" \"arg2\"] } \"name1\""); //$NON-NLS-1$
		// bad permission; missing type
		checkBadInfo("ALLOW { [Test1 \"arg1\" \"arg2\"] () } \"name1\""); //$NON-NLS-1$
		checkBadInfo("ALLOW { [Test1 \"arg1\" \"arg2\"] (\"name1\" \"action1\") } \"name1\""); //$NON-NLS-1$
		// bad name; no quotes
		checkBadInfo("AlLoW { [Test1 \"arg1\" \"arg2\"] (Type1 \"name1\" \"action1\") } name1"); //$NON-NLS-1$
		// bad name; missing end quote
		checkBadInfo("AlLoW { [Test1 \"arg1\" \"arg2\"] (Type1 \"name1\" \"action1\") } \"name1"); //$NON-NLS-1$
		// bad name; missing start quote
		checkBadInfo("AlLoW { [Test1 \"arg1\" \"arg2\"] (Type1 \"name1\" \"action1\") } name1\""); //$NON-NLS-1$
		// bad name; single quote
		checkBadInfo("AlLoW { [Test1 \"arg1\" \"arg2\"] (Type1 \"name1\" \"action1\") } \""); //$NON-NLS-1$
		// bad name; extra stuff
		checkBadInfo("AlLoW { [Test1 \"arg1\" \"arg2\"] (Type1 \"name1\" \"action1\") } \"name1\" extrajunk"); //$NON-NLS-1$

	}

	public void testBug286307() {
		Bundle test = installTestBundle("test.bug286307"); //$NON-NLS-1$
		AccessControlContext acc = test.adapt(AccessControlContext.class);
		testPermission(acc, new FilePermission("test", "read"), true); //$NON-NLS-1$ //$NON-NLS-2$
		testPermission(acc, new AllPermission(), false);
	}

	private void checkInfos(ConditionalPermissionInfo testInfo1, ConditionalPermissionInfo testInfo2) {
		assertTrue("Infos are not equal: " + testInfo1.getEncoded() + " " + testInfo2.getEncoded(), testInfo1.equals(testInfo2)); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Info hash code is not equal", testInfo1.hashCode(), testInfo2.hashCode()); //$NON-NLS-1$
	}

	private void checkBadInfo(String encoded) {
		try {
			cpa.newConditionalPermissionInfo(encoded);
			fail("Expecting fail with bad info: " + encoded); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	private ConditionalPermissionInfo checkGoodInfo(String encoded) {
		try {
			return cpa.newConditionalPermissionInfo(encoded);
		} catch (IllegalArgumentException e) {
			fail("Unexpected failure with good info: " + encoded, e); //$NON-NLS-1$
		}
		return null;
	}

	private void testSMPermission(ProtectionDomain[] pds, Permission permission, boolean expectedToPass) {
		AccessControlContext acc = new AccessControlContext(pds);
		try {
			SecurityManager sm = System.getSecurityManager();
			sm.checkPermission(permission, acc);
			if (!expectedToPass)
				fail("test should not have the permission " + permission); //$NON-NLS-1$
		} catch (SecurityException e) {
			if (expectedToPass)
				fail("test should have the permission " + permission); //$NON-NLS-1$
		}
	}

	private void testPermission(AccessControlContext acc, Permission permission, boolean expectedToPass) {
		try {
			SecurityManager sm = System.getSecurityManager();
			sm.checkPermission(permission, acc);
			if (!expectedToPass) {
				fail("test should not have the permission " + permission); //$NON-NLS-1$
			}
		} catch (AccessControlException e) {
			if (expectedToPass) {
				fail("test should have the permission " + permission); //$NON-NLS-1$
			}
		}
	}

	private ConditionInfo[] getLocationConditions(String location, boolean not) {
		String[] args = not ? new String[] {location, "!"} : new String[] {location}; //$NON-NLS-1$
		return new ConditionInfo[] {new ConditionInfo("org.osgi.service.condpermadmin.BundleLocationCondition", args)}; //$NON-NLS-1$
	}

	private Bundle installTestBundle(String name) {
		try {
			String location = installer.getBundleLocation(name);
			return equinox.getBundleContext().installBundle(location);
		} catch (BundleException e) {
			fail("failed to install bundle: " + name, e); //$NON-NLS-1$
		}
		return null;
	}

	private void installConditionBundle() {
		try {
			Bundle bundle = installer.installBundle("ext.framework.b", false); //$NON-NLS-1$
			installer.resolveBundles(new Bundle[] {bundle});

		} catch (BundleException e) {
			fail("failed to install bundle", e); //$NON-NLS-1$
		}
	}
}
