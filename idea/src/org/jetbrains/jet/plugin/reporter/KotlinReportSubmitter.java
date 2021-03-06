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

package org.jetbrains.jet.plugin.reporter;

import com.intellij.diagnostic.ITNReporter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;

/**
 * We need to wrap ITNReporter for force showing or errors from kotlin plugin even from released version of IDEA.
 *
 * @author abreslav
 */
public class KotlinReportSubmitter extends ITNReporter {
    @Override
    public boolean showErrorInRelease(IdeaLoggingEvent event) {
        return true;
    }
}
