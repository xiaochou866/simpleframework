package org.simpleframework.aop.aspect;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.simpleframework.aop.PointcutLocator;

import java.awt.*;

@AllArgsConstructor
@Getter
public class AspectInfo {
    private int orderIndex;
    private DefaultAspect aspectObject;

    private PointcutLocator pointcutLocator;
}
