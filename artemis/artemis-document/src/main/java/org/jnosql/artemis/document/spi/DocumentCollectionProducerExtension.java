/*
 *  Copyright (c) 2017 Otávio Santana and others
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.jnosql.artemis.document.spi;


import jakarta.nosql.mapping.ConfigurationUnit;
import org.jnosql.artemis.DatabaseMetadata;
import org.jnosql.artemis.Databases;
import jakarta.nosql.mapping.Repository;
import jakarta.nosql.mapping.RepositoryAsync;
import org.jnosql.artemis.document.query.RepositoryAsyncDocumentBean;
import org.jnosql.artemis.document.query.RepositoryDocumentBean;
import org.jnosql.artemis.util.ConfigurationUnitUtils;
import org.jnosql.artemis.util.RepositoryUnit;
import jakarta.nosql.document.DocumentCollectionManager;
import jakarta.nosql.document.DocumentCollectionManagerAsync;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessProducer;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import static jakarta.nosql.mapping.DatabaseType.DOCUMENT;

/**
 * Extension to start up the DocumentTemplate, DocumentTemplateAsync, Repository and RepositoryAsync
 * from the {@link jakarta.nosql.mapping.Database} qualifier
 */
public class DocumentCollectionProducerExtension implements Extension {

    private static final Logger LOGGER = Logger.getLogger(DocumentCollectionProducerExtension.class.getName());

    private final Set<DatabaseMetadata> databases = new HashSet<>();

    private final Set<DatabaseMetadata> databasesAsync = new HashSet<>();

    private final Collection<Class<?>> crudTypes = new HashSet<>();

    private final Collection<Class<?>> crudAsyncTypes = new HashSet<>();

    private final Collection<RepositoryUnit> repositoryUnits = new HashSet<>();

    private final Collection<RepositoryUnit> repositoryAsyncUnits = new HashSet<>();


    <T extends Repository> void observes(@Observes final ProcessAnnotatedType<T> repo) {
        Class<T> javaClass = repo.getAnnotatedType().getJavaClass();
        if (Repository.class.equals(javaClass)) {
            return;
        }


        if (Arrays.asList(javaClass.getInterfaces()).contains(Repository.class)
                && Modifier.isInterface(javaClass.getModifiers())) {
            LOGGER.info("Adding a new Repository as discovered on document: " + javaClass);
            crudTypes.add(javaClass);
        }
    }

    <T extends RepositoryAsync> void observesAsync(@Observes final ProcessAnnotatedType<T> repo) {
        Class<T> javaClass = repo.getAnnotatedType().getJavaClass();

        if (RepositoryAsync.class.equals(javaClass)) {
            return;
        }

        if (Arrays.asList(javaClass.getInterfaces()).contains(RepositoryAsync.class)
                && Modifier.isInterface(javaClass.getModifiers())) {
            LOGGER.info("Adding a new RepositoryAsync as discovered on document: " + javaClass);
            crudAsyncTypes.add(javaClass);
        }
    }

    <T, X extends DocumentCollectionManager> void observes(@Observes final ProcessProducer<T, X> pp) {
        Databases.addDatabase(pp, DOCUMENT, databases);
    }

    <T, X extends DocumentCollectionManagerAsync> void observesAsync(@Observes final ProcessProducer<T, X> pp) {
        Databases.addDatabase(pp, DOCUMENT, databasesAsync);
    }

    <T, R extends Repository<?, ?>> void observes(@Observes ProcessInjectionPoint<T, R> event) {
        observes(event.getInjectionPoint(), repositoryUnits);
    }

    <T, R extends RepositoryAsync<?, ?>> void observesAsync(@Observes ProcessInjectionPoint<T, R> event) {
        observes(event.getInjectionPoint(), repositoryAsyncUnits);
    }

    private void observes(InjectionPoint injectionPoint, Collection<RepositoryUnit> repositoryAsyncUnits) {

        if (ConfigurationUnitUtils.hasConfigurationUnit(injectionPoint)) {

            ConfigurationUnit configurationUnit = ConfigurationUnitUtils.getConfigurationUnit(injectionPoint);
            Type type = injectionPoint.getType();
            RepositoryUnit unitRepository = RepositoryUnit.of((Class<?>) type, configurationUnit);
            if (unitRepository.isDocument()) {
                LOGGER.info(String.format("Found Repository to configuration unit document to configuration name %s fileName %s database: %s repository: %s",
                        configurationUnit.name(), configurationUnit.fileName(), configurationUnit.database(), type.toString()));
                repositoryAsyncUnits.add(unitRepository);

            }
        }
    }

    void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery afterBeanDiscovery, final BeanManager beanManager) {
        LOGGER.info(String.format("Starting to process on documents: %d databases crud %d and crudAsync %d",
                databases.size(), crudTypes.size(), crudAsyncTypes.size()));

        databases.forEach(type -> {
            final TemplateBean bean = new TemplateBean(beanManager, type.getProvider());
            afterBeanDiscovery.addBean(bean);
        });

        databasesAsync.forEach(type -> {
            final TemplateAsyncBean bean = new TemplateAsyncBean(beanManager, type.getProvider());
            afterBeanDiscovery.addBean(bean);
        });

        crudTypes.forEach(type -> {
            if (!databases.contains(DatabaseMetadata.DEFAULT_DOCUMENT)) {
                afterBeanDiscovery.addBean(new RepositoryDocumentBean(type, beanManager, ""));
            }
            databases.forEach(database -> {
                final RepositoryDocumentBean bean = new RepositoryDocumentBean(type, beanManager, database.getProvider());
                afterBeanDiscovery.addBean(bean);
            });
        });

        crudAsyncTypes.forEach(type -> {
            if (!databases.contains(DatabaseMetadata.DEFAULT_DOCUMENT)) {
                afterBeanDiscovery.addBean(new RepositoryAsyncDocumentBean(type, beanManager, ""));
            }
            databasesAsync.forEach(database -> {
                final RepositoryAsyncDocumentBean bean = new RepositoryAsyncDocumentBean(type, beanManager,
                        database.getProvider());
                afterBeanDiscovery.addBean(bean);
            });
        });

        repositoryUnits.forEach(type -> afterBeanDiscovery.addBean(new RepositoryUnitDocumentBean(beanManager, type)));

        repositoryAsyncUnits.forEach(type -> afterBeanDiscovery.addBean(new RepositoryAsyncUnitDocumentBean(beanManager, type)));

    }

}
