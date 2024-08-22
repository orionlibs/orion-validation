package io.github.orionlibs.orion_validation;

import io.github.orionlibs.orion_reflection.variable.access.ReflectionInstanceVariablesAccessService;
import io.github.orionlibs.orion_reflection.variable.retrieval.ReflectionInstanceVariablesRetrievalService;
import io.github.orionlibs.orion_validation.annotation.InRange;
import io.github.orionlibs.orion_validation.annotation.NotBlank;
import io.github.orionlibs.orion_validation.annotation.NotEmpty;
import io.github.orionlibs.orion_validation.annotation.NotNull;
import io.github.orionlibs.orion_validation.annotation.NotNullOrBlank;
import io.github.orionlibs.orion_validation.annotation.WithRegEx;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// @NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
//@Setter
public class ValidationBO
{
    private Object object;


    public static ValidationBO of(Object object)
    {
        return ValidationBO.builder().object(object).build();
    }


    public InvalidFields validate()
    {
        Set<String> invalidInstanceVariableNames = new HashSet<>();
        if(object != null)
        {
            List<Field> instanceVariables = ReflectionInstanceVariablesRetrievalService.getAllInstanceVariables(object);
            instanceVariables.forEach(field ->
            {
                ReflectionInstanceVariablesAccessService.makeInstanceVariableAccessible(field);
                Object data = null;
                try
                {
                    data = field.get(object);
                    if(data instanceof OrionInvalidatable)
                    {
                        InvalidFields invalidFields = ValidationBO.of(data).validate();
                        if(invalidFields.isEmpty())
                        {
                            for(String invalidField : invalidFields.getFields())
                            {
                                invalidInstanceVariableNames.add(field.getName() + "." + invalidField);
                            }
                        }
                    }
                    else if(data instanceof Collection<?>)
                    {
                        Collection<?> dataList = (Collection<?>)data;
                        outterLoop:
                        for(Object dataObject : dataList)
                        {
                            if(dataObject instanceof OrionInvalidatable)
                            {
                                InvalidFields invalidFields = ValidationBO.of(dataObject).validate();
                                if(invalidFields.isNotEmpty())
                                {
                                    for(String invalidField : invalidFields.getFields())
                                    {
                                        invalidInstanceVariableNames.add(field.getName() + "." + invalidField);
                                    }
                                }
                            }
                        }
                    }
                }
                catch(IllegalAccessException e)
                {
                    throw new RuntimeException(e);
                }
                try
                {
                    if(!isInstanceVariableValid(field, object))
                    {
                        invalidInstanceVariableNames.add(field.getName());
                    }
                }
                catch(IllegalArgumentException e)
                {
                    throw e;
                }
                catch(IllegalAccessException e)
                {
                    //throw new InaccessibleException("The instance variable is inaccessible.");
                }
            });
        }
        return InvalidFields.builder()
                        .fields(invalidInstanceVariableNames)
                        .build();
    }


    @SuppressWarnings("rawtypes")
    private static boolean isInstanceVariableValid(Field instanceVariable, Object object) throws IllegalArgumentException, IllegalAccessException
    {
        boolean isInstanceVariableValid = true;
        ReflectionInstanceVariablesAccessService.makeInstanceVariableAccessible(instanceVariable);
        Object data = instanceVariable.get(object);
        NotNullOrBlank notNullOrBlankAnnotation = instanceVariable.getAnnotation(NotNullOrBlank.class);
        NotNull notNullAnnotation = instanceVariable.getAnnotation(NotNull.class);
        NotEmpty notEmptyAnnotation = instanceVariable.getAnnotation(NotEmpty.class);
        NotBlank notBlankAnnotation = instanceVariable.getAnnotation(NotBlank.class);
        InRange inRangeAnnotation = instanceVariable.getAnnotation(InRange.class);
        WithRegEx withRegExAnnotation = instanceVariable.getAnnotation(WithRegEx.class);
        if(notNullOrBlankAnnotation != null)
        {
            if(data == null)
            {
                isInstanceVariableValid = false;
            }
            else if(data instanceof String)
            {
                String dataString = (String)data;
                isInstanceVariableValid = isInstanceVariableValid && dataString != null && !dataString.trim().isEmpty();
            }
        }
        if(notNullAnnotation != null)
        {
            isInstanceVariableValid = isInstanceVariableValid && data != null;
        }
        if(notEmptyAnnotation != null && isInstanceVariableValid)
        {
            if(data instanceof String)
            {
                String dataString = (String)data;
                isInstanceVariableValid = isInstanceVariableValid && dataString != null && !dataString.isEmpty();
            }
            else if(data instanceof List)
            {
                List dataList = (List)data;
                isInstanceVariableValid = isInstanceVariableValid && dataList != null && !dataList.isEmpty();
            }
            else
            {
                isInstanceVariableValid = false;
            }
        }
        if(notBlankAnnotation != null && isInstanceVariableValid)
        {
            if(data instanceof String)
            {
                String dataString = (String)data;
                isInstanceVariableValid = isInstanceVariableValid && dataString != null && !dataString.trim().isEmpty();
            }
            else
            {
                isInstanceVariableValid = false;
            }
        }
        if(inRangeAnnotation != null)
        {
            BigDecimal tempData = null;
            if(data instanceof Number)
            {
                tempData = new BigDecimal(((Number)data).toString());
            }
            else if(data instanceof String)
            {
                tempData = new BigDecimal(((String)data).toString());
            }
            BigDecimal inRangeMinimumValue = new BigDecimal(inRangeAnnotation.min());
            BigDecimal inRangeMaximumValue = new BigDecimal(inRangeAnnotation.max());
            if(tempData.compareTo(inRangeMinimumValue) >= 0 && tempData.compareTo(inRangeMaximumValue) <= 0)
            {
                isInstanceVariableValid = true;
            }
            else
            {
                isInstanceVariableValid = false;
            }
        }
        if(withRegExAnnotation != null)
        {
            if(data == null)
            {
                isInstanceVariableValid = false;
            }
            else if(data instanceof String)
            {
                String dataString = (String)data;
                String[] regexPatterns = withRegExAnnotation.value();
                isInstanceVariableValid = false;
                for(String regexPattern : regexPatterns)
                {
                    Pattern matcher = Pattern.compile(regexPattern);
                    if(matcher.matcher(dataString).matches())
                    {
                        isInstanceVariableValid = true;
                        break;
                    }
                }
            }
            else if(data instanceof Collection<?>)
            {
                Collection<?> dataList = (Collection<?>)data;
                String[] regexPatterns = withRegExAnnotation.value();
                isInstanceVariableValid = true;
                outterLoop:
                for(Object dataObject : dataList)
                {
                    if(dataObject instanceof String)
                    {
                        String dataString = (String)dataObject;
                        boolean foundMatch = false;
                        for(String regexPattern : regexPatterns)
                        {
                            Pattern matcher = Pattern.compile(regexPattern);
                            if(matcher.matcher(dataString).matches())
                            {
                                foundMatch = true;
                                break;
                            }
                        }
                        if(!foundMatch)
                        {
                            isInstanceVariableValid = false;
                            break outterLoop;
                        }
                    }
                    else
                    {
                        isInstanceVariableValid = false;
                        break outterLoop;
                    }
                }
            }
        }
        return isInstanceVariableValid;
    }
}