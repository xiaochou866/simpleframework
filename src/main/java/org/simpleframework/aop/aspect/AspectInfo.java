package org.simpleframework.aop.aspect;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
public class AspectInfo {
    private int orderIndex;
    private DefaultAspect aspectObject;
}
