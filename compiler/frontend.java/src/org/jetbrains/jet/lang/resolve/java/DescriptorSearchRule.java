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

package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;

/**
 * @author Stepan Koltsov
 */
public enum DescriptorSearchRule {
    INCLUDE_KOTLIN {
        @Override
        public <T extends DeclarationDescriptor> T processFoundInKotlin(T foundDescriptor) {
            return foundDescriptor;
        }
    },
    IGNORE_IF_FOUND_IN_KOTLIN {
        @Override
        public <T extends DeclarationDescriptor> T processFoundInKotlin(T foundDescriptor) {
            return null;
        }
    },
    ERROR_IF_FOUND_IN_KOTLIN {
        @Override
        public <T extends DeclarationDescriptor> T processFoundInKotlin(T foundDescriptor) {
            throw new IllegalStateException(DescriptorUtils.getFQName(foundDescriptor) + " should not be found in Kotlin.");
        }
    };

    public abstract <T extends DeclarationDescriptor> T processFoundInKotlin(T foundDescriptor);
}
