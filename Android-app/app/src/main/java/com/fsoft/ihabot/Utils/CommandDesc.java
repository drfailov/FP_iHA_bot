package com.fsoft.ihabot.Utils;
/**
 * Этот класс представляет собой элемент справки
 * Created by Dr. Failov on 13.07.2017.
 */

public class CommandDesc {
    private String name = "";
    private String helpText = "";
    private String example = "";

    public CommandDesc(String name, String helpText, String example) {
        this.name = name;
        this.helpText = helpText;
        this.example = example;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }
}
