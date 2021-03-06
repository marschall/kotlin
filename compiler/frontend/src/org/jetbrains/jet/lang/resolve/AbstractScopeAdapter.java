/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.name.LabelName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.util.Collection;
import java.util.List;

/**
 * Introduces a simple wrapper for internal scope.
 *
 * @author abreslav
 */
public abstract class AbstractScopeAdapter implements JetScope {
    @NotNull
    protected abstract JetScope getWorkerScope();

    @NotNull
    @Override
    public List<ReceiverParameterDescriptor> getImplicitReceiversHierarchy() {
        return getWorkerScope().getImplicitReceiversHierarchy();
    }

    @NotNull
    @Override
    public Collection<FunctionDescriptor> getFunctions(@NotNull Name name) {
        return getWorkerScope().getFunctions(name);
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull Name name) {
        return getWorkerScope().getNamespace(name);
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull Name name) {
        return getWorkerScope().getClassifier(name);
    }

    @Override
    public ClassDescriptor getObjectDescriptor(@NotNull Name name) {
        return getWorkerScope().getObjectDescriptor(name);
    }

    @NotNull
    @Override
    public Collection<ClassDescriptor> getObjectDescriptors() {
        return getWorkerScope().getObjectDescriptors();
    }

    @NotNull
    @Override
    public Collection<VariableDescriptor> getProperties(@NotNull Name name) {
        return getWorkerScope().getProperties(name);
    }

    @Override
    public VariableDescriptor getLocalVariable(@NotNull Name name) {
        return getWorkerScope().getLocalVariable(name);
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return getWorkerScope().getContainingDeclaration();
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(LabelName labelName) {
        return getWorkerScope().getDeclarationsByLabel(labelName);
    }

    @Override
    public PropertyDescriptor getPropertyByFieldReference(@NotNull Name fieldName) {
        return getWorkerScope().getPropertyByFieldReference(fieldName);
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        return getWorkerScope().getAllDescriptors();
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getOwnDeclaredDescriptors() {
        return getWorkerScope().getOwnDeclaredDescriptors();
    }
}
