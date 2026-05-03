package com.sparktf.vo.actions.sink;

import com.sparktf.vo.actions.Action;
import lombok.Data;


@Data
public abstract class Sink extends Action {
    private String input;
}
