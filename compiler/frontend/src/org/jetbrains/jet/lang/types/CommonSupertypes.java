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

package org.jetbrains.jet.lang.types;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.utils.DFS;

import java.util.*;

import static org.jetbrains.jet.lang.types.Variance.*;

/**
* @author abreslav
*/
public class CommonSupertypes {
    @NotNull
    public static JetType commonSupertype(@NotNull Collection<JetType> types) {
        Collection<JetType> typeSet = new HashSet<JetType>(types);
        assert !typeSet.isEmpty();

        // If any of the types is nullable, the result must be nullable
        // This also removed Nothing and Nothing? because they are subtypes of everything else
        boolean nullable = false;
        for (Iterator<JetType> iterator = typeSet.iterator(); iterator.hasNext();) {
            JetType type = iterator.next();
            assert type != null;
            if (KotlinBuiltIns.getInstance().isNothingOrNullableNothing(type)) {
                iterator.remove();
            }
            nullable |= type.isNullable();
        }

        // Everything deleted => it's Nothing or Nothing?
        if (typeSet.isEmpty()) {
            // TODO : attributes
            return nullable ? KotlinBuiltIns.getInstance().getNullableNothingType() : KotlinBuiltIns.getInstance().getNothingType();
        }

        if (typeSet.size() == 1) {
            return TypeUtils.makeNullableIfNeeded(typeSet.iterator().next(), nullable);
        }

        // constructor of the supertype -> all of its instantiations occurring as supertypes
        Map<TypeConstructor, Set<JetType>> commonSupertypes = computeCommonRawSupertypes(typeSet);
        while (commonSupertypes.size() > 1) {
            Set<JetType> merge = new HashSet<JetType>();
            for (Set<JetType> supertypes : commonSupertypes.values()) {
                merge.addAll(supertypes);
            }
            commonSupertypes = computeCommonRawSupertypes(merge);
        }
        assert !commonSupertypes.isEmpty() : commonSupertypes + " <- " + types;

        // constructor of the supertype -> all of its instantiations occurring as supertypes
        Map.Entry<TypeConstructor, Set<JetType>> entry = commonSupertypes.entrySet().iterator().next();

        // Reconstructing type arguments if possible
        JetType result = computeSupertypeProjections(entry.getKey(), entry.getValue());
        return TypeUtils.makeNullableIfNeeded(result, nullable);
    }

    // Raw supertypes are superclasses w/o type arguments
    // @return TypeConstructor -> all instantiations of this constructor occurring as supertypes
    @NotNull
    private static Map<TypeConstructor, Set<JetType>> computeCommonRawSupertypes(@NotNull Collection<JetType> types) {
        assert !types.isEmpty();

        final Map<TypeConstructor, Set<JetType>> constructorToAllInstances = new HashMap<TypeConstructor, Set<JetType>>();
        Set<TypeConstructor> commonSuperclasses = null;

        List<TypeConstructor> order = null;
        for (JetType type : types) {
            Set<TypeConstructor> visited = Sets.newHashSet();
            order = topologicallySortSuperclassesAndRecordAllInstances(type, constructorToAllInstances, visited);

            if (commonSuperclasses == null) {
                commonSuperclasses = visited;
            }
            else {
                commonSuperclasses.retainAll(visited);
            }
        }
        assert order != null;

        Set<TypeConstructor> notSource = new HashSet<TypeConstructor>();
        Map<TypeConstructor, Set<JetType>> result = new HashMap<TypeConstructor, Set<JetType>>();
        for (TypeConstructor superConstructor : order) {
            if (!commonSuperclasses.contains(superConstructor)) {
                continue;
            }

            if (!notSource.contains(superConstructor)) {
                result.put(superConstructor, constructorToAllInstances.get(superConstructor));
                markAll(superConstructor, notSource);
            }
        }

        return result;
    }

    private static List<TypeConstructor> topologicallySortSuperclassesAndRecordAllInstances(
            @NotNull JetType type,
            @NotNull final Map<TypeConstructor, Set<JetType>> constructorToAllInstances,
            @NotNull final Set<TypeConstructor> visited
    ) {
        return DFS.dfs(
                Collections.singletonList(type),
                new DFS.Neighbors<JetType>() {
                    @NotNull
                    @Override
                    public Iterable<JetType> getNeighbors(JetType current) {
                        TypeSubstitutor substitutor = TypeSubstitutor.create(current);
                        List<JetType> result = Lists.newArrayList();
                        for (JetType supertype : current.getConstructor().getSupertypes()) {
                            if (visited.contains(supertype.getConstructor())) {
                                continue;
                            }
                            result.add(substitutor.safeSubstitute(supertype, INVARIANT));
                        }
                        return result;
                    }
                },
                new DFS.Visited<JetType>() {
                    @Override
                    public boolean checkAndMarkVisited(JetType current) {
                        return visited.add(current.getConstructor());
                    }
                },
                new DFS.NodeHandlerWithListResult<JetType, TypeConstructor>() {
                    @Override
                    public void beforeChildren(JetType current) {
                        TypeConstructor constructor = current.getConstructor();

                        Set<JetType> instances = constructorToAllInstances.get(constructor);
                        if (instances == null) {
                            instances = new HashSet<JetType>();
                            constructorToAllInstances.put(constructor, instances);
                        }
                        instances.add(current);
                    }

                    @Override
                    public void afterChildren(JetType current) {
                        result.addFirst(current.getConstructor());
                    }
                }
        );
    }

    // constructor - type constructor of a supertype to be instantiated
    // types - instantiations of constructor occurring as supertypes of classes we are trying to intersect
    @NotNull
    private static JetType computeSupertypeProjections(@NotNull TypeConstructor constructor, @NotNull Set<JetType> types) {
        // we assume that all the given types are applications of the same type constructor

        assert !types.isEmpty();

        if (types.size() == 1) {
            return types.iterator().next();
        }

        List<TypeParameterDescriptor> parameters = constructor.getParameters();
        List<TypeProjection> newProjections = new ArrayList<TypeProjection>();
        for (int i = 0, parametersSize = parameters.size(); i < parametersSize; i++) {
            TypeParameterDescriptor parameterDescriptor = parameters.get(i);
            Set<TypeProjection> typeProjections = new HashSet<TypeProjection>();
            for (JetType type : types) {
                typeProjections.add(type.getArguments().get(i));
            }
            newProjections.add(computeSupertypeProjection(parameterDescriptor, typeProjections));
        }

        boolean nullable = false;
        for (JetType type : types) {
            nullable |= type.isNullable();
        }

        // TODO : attributes?
        JetScope newScope = KotlinBuiltIns.getInstance().STUB;
        DeclarationDescriptor declarationDescriptor = constructor.getDeclarationDescriptor();
        if (declarationDescriptor instanceof ClassDescriptor) {
            newScope = ((ClassDescriptor) declarationDescriptor).getMemberScope(newProjections);
        }
        return new JetTypeImpl(Collections.<AnnotationDescriptor>emptyList(), constructor, nullable, newProjections, newScope);
    }

    @NotNull
    private static TypeProjection computeSupertypeProjection(@NotNull TypeParameterDescriptor parameterDescriptor, @NotNull Set<TypeProjection> typeProjections) {
        if (typeProjections.size() == 1) {
            return typeProjections.iterator().next();
        }

        Set<JetType> ins = new HashSet<JetType>();
        Set<JetType> outs = new HashSet<JetType>();

        Variance variance = parameterDescriptor.getVariance();
        switch (variance) {
            case INVARIANT:
                // Nothing
                break;
            case IN_VARIANCE:
                outs = null;
                break;
            case OUT_VARIANCE:
                ins = null;
                break;
        }

        for (TypeProjection projection : typeProjections) {
            Variance projectionKind = projection.getProjectionKind();
            if (projectionKind.allowsInPosition()) {
                if (ins != null) {
                    ins.add(projection.getType());
                }
            }
            else {
                ins = null;
            }

            if (projectionKind.allowsOutPosition()) {
                if (outs != null) {
                    outs.add(projection.getType());
                }
            }
            else {
                outs = null;
            }
        }

        if (ins != null) {
            JetType intersection = TypeUtils.intersect(JetTypeChecker.INSTANCE, ins);
            if (intersection == null) {
                if (outs != null) {
                    return new TypeProjection(OUT_VARIANCE, commonSupertype(outs));
                }
                return new TypeProjection(OUT_VARIANCE, commonSupertype(parameterDescriptor.getUpperBounds()));
            }
            Variance projectionKind = variance == IN_VARIANCE ? Variance.INVARIANT : IN_VARIANCE;
            return new TypeProjection(projectionKind, intersection);
        }
        else if (outs != null) {
            Variance projectionKind = variance == OUT_VARIANCE ? Variance.INVARIANT : OUT_VARIANCE;
            return new TypeProjection(projectionKind, commonSupertype(outs));
        }
        else {
            Variance projectionKind = variance == OUT_VARIANCE ? Variance.INVARIANT : OUT_VARIANCE;
            return new TypeProjection(projectionKind, commonSupertype(parameterDescriptor.getUpperBounds()));
        }
    }

    private static void markAll(@NotNull TypeConstructor typeConstructor, @NotNull Set<TypeConstructor> markerSet) {
        markerSet.add(typeConstructor);
        for (JetType type : typeConstructor.getSupertypes()) {
            markAll(type.getConstructor(), markerSet);
        }
    }
}
