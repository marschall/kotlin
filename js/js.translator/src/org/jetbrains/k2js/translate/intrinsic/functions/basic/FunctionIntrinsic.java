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

package org.jetbrains.k2js.translate.intrinsic.functions.basic;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.k2js.translate.context.TranslationContext;

import java.util.List;

/**
 * @author Pavel Talanov
 *         <p/>
 *         Base for intrinsics that substitute standard function calls like Int#plus, Float#minus ... etc
 */
public abstract class FunctionIntrinsic {

    @NotNull
    public static final FunctionIntrinsic NO_INTRINSIC = new FunctionIntrinsic() {
        @Override
        public boolean exists() {
            return false;
        }

        @NotNull
        @Override
        public JsExpression apply(@Nullable JsExpression receiver,
                @NotNull List<JsExpression> arguments,
                @NotNull TranslationContext context) {
            throw new UnsupportedOperationException("FunctionIntrinsic#NO_INTRINSIC_#apply");
        }
    };

    @NotNull
    public abstract JsExpression apply(@Nullable JsExpression receiver, @NotNull List<JsExpression> arguments,
            @NotNull TranslationContext context);

    public boolean exists() {
        return true;
    }
}
