/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.ejb.transaction.cmt.timeout.xa;

import javax.annotation.Resource;
import javax.ejb.AfterBegin;
import javax.ejb.AfterCompletion;
import javax.ejb.BeforeCompletion;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.as.test.integration.transactions.TxTestUtil;
import org.jboss.logging.Logger;

/**
 * It's not expected {@link AfterBegin}, {@link BeforeCompletion} and {@link AfterCompletion}
 * would work for {@link Stateless} bean. This works only for {@link Stateful} one.
 */
@Stateless
public class StatelessWithAnnotationBean {
    private static final Logger log = Logger.getLogger(StatelessWithAnnotationBean.class);

    @Resource(name = "java:jboss/TransactionManager")
    private TransactionManager tm;

    @Inject
    private TransactionCheckerSingleton checker;

    @AfterBegin
    public void afterBegin() {
        log.info("afterBegin called");
        checker.setSynchronizedBegin();
    }

    @BeforeCompletion
    public void beforeCompletion() {
        log.info("beforeCompletion called");
        checker.setSynchronizedBefore();
    }

    @AfterCompletion
    public void afterCompletion(boolean isCommitted) {
        log.infof("afterCompletion: transaction was%s committed", isCommitted ? "" : " not");
        checker.setSynchronizedAfter(isCommitted);
    }

    public void testTransaction() throws SystemException, InterruptedException {
        Transaction txn;
        txn = tm.getTransaction();
        
        TxTestUtil.enlistTestXAResource(txn); 
        TxTestUtil.enlistTestXAResource(txn);
    }
}