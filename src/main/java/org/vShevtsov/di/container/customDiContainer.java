package org.vShevtsov.di.container;

import lombok.SneakyThrows;
import org.burningwave.core.assembler.ComponentContainer;
import org.burningwave.core.assembler.ComponentSupplier;
import org.burningwave.core.classes.ClassHunter;
import org.burningwave.core.classes.SearchConfig;
import org.vShevtsov.di.container.annotations.*;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;

import static org.burningwave.core.assembler.StaticComponentContainer.Fields;

public class customDiContainer {
    private Map<Class<?>, Object> cache;
    private Map<Class<?>, Class<?>> interfacesMap;

    public customDiContainer() {
        this.cache = new HashMap<>();
        this.interfacesMap = new HashMap<>();
    }

    @SneakyThrows
    public void initClasses() {
        Collection<Class<?>> classes = findAllClasses();
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Component.class)) {
                appendInterface(clazz);
                Object classInstance = clazz.getDeclaredConstructor().newInstance();
                cache.put(clazz, classInstance);
                checkForAutowiredAndQualifier(clazz,classInstance);
                CallTheMethod(clazz);
            }
        }
    }

    public Collection<Class<?>> findAllClasses() {
        ComponentSupplier componentSupplier = ComponentContainer.getInstance();
        ClassHunter classHunter = componentSupplier.getClassHunter();

        SearchConfig searchConfig = SearchConfig.forResources("org/vShevtsov/di/container");
        try (ClassHunter.SearchResult result = classHunter.findBy(searchConfig)) {
            Collection<Class<?>> classes = result.getClasses();
            return Arrays.asList(classes.toArray(new Class[classes.size()]));
        }
    }
    public void appendInterface(Class<?> clazz){
        for (Class<?> interfacez:clazz.getInterfaces()) {
            interfacesMap.put(clazz,interfacez);
        }
    }
    @SneakyThrows
    public void CallTheMethod(Class<?> clazz) {
        getAllPostConstructMethods(clazz).stream().findFirst().get().invoke(cache.get(clazz));
    }

    public List<Method> getAllPostConstructMethods(Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        List<Method> withAnnotation = new ArrayList<>();
        for (Method method : methods) {
            if (method.isAnnotationPresent(PostConstruct.class)) {
                withAnnotation.add(method);
            }
        }
        return withAnnotation;
    }
    public void checkForAutowiredAndQualifier(Class<?> clazz,Object instance) throws Exception {
        for(Field field: getAllAutowired(clazz)){
            if(field.isAnnotationPresent(Qualifier.class)){
                String val = field.getAnnotation(Qualifier.class).value();
                Object fieldInstance = getReadyBean(field.getType(), field.getName(), val);
                Fields.setDirect(clazz, field, fieldInstance);
                checkForAutowiredAndQualifier(fieldInstance.getClass(), fieldInstance);
            }
        }
    }
    public Collection<Field> getAllAutowired(Class<?> clazz)
    {
        Collection<Field> fields = new ArrayList<>();
        for(Field fields1: clazz.getDeclaredFields()){
            if(fields1.isAnnotationPresent(Autowired.class)){
                fields.add(fields1);
            }
        }
        return fields;
    }
    public <T> Object getReadyBean(Class<T> interfaceClass, String fieldName, String qualifier) throws Exception {
        Class<?> implementationClass = getInstance(interfaceClass, fieldName, qualifier);
        if (cache.containsKey(implementationClass)){
            return cache.get(implementationClass);
        }
        else {
            Object classInstance = interfaceClass.getDeclaredConstructor().newInstance();
            cache.put(implementationClass, classInstance);
            CallTheMethod((Class<?>) classInstance);
            return classInstance;
        }
    }
    public Class<?> getInstance(Class<?> interfacez,String name,String qualifier) throws Exception {
        Set<Entry<Class<?>, Class<?>>> classes = new HashSet<>();
        for (Entry<Class<?>, Class<?>> entry : interfacesMap.entrySet()) {
            if (entry.getValue() == interfacez) {
                classes.add(entry);
            }
        }
        if (classes.size() == 0) {
            throw new Exception(new Error("Cannot find instance for class " + interfacez.getName()));
        }
        if (classes.size() == 1) {
            Optional<Entry<Class<?>, Class<?>>> optional = classes.stream().findFirst();
            if (optional.isPresent()) {
                return optional.get().getKey();
            }
        } else if (classes.size() > 1) {
            final String findBy = (qualifier == null || qualifier.trim().length() == 0) ? name : qualifier;
            Optional<Entry<Class<?>, Class<?>>> optional = classes.stream()
                    .filter(entry -> entry.getKey().getSimpleName().equalsIgnoreCase(findBy)).findAny();
            if (optional.isPresent()) {
                return optional.get().getKey();
            } else {
                throw new Exception(new Error("Some error"));
            }
        }
        return null;
    }

}
