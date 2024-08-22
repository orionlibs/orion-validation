package io.github.orionlibs.orion_validation;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class InvalidFields
{
    private Set<String> fields;


    public boolean isEmpty()
    {
        return fields != null && fields.isEmpty();
    }


    public boolean isNotEmpty()
    {
        return !isEmpty();
    }


    public void add(String field)
    {
        fields.add(field);
    }
}