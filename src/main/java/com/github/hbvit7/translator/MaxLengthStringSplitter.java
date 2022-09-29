package com.github.hbvit7.translator;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MaxLengthStringSplitter {

    private final int maxLenght;

    public MaxLengthStringSplitter(int maxLenght) {
        this.maxLenght = maxLenght;
    }

    public static void main(String[] args) {
        MaxLengthStringSplitter maxLengthStringSplitter = new MaxLengthStringSplitter(47);
        String data = "Hello there, my name is not importnant right now."
                + " I am just simple sentecne used to test few things.";
        System.out.println(maxLengthStringSplitter.split(data));
    }

    public String split(String data) {

        if (StringUtils.isBlank(data)) {
            return data;
        }

        Pattern pattern = Pattern.compile("\\G\\s*(.{1," + maxLenght + "})(?=\\s|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(data);
        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            String group = matcher.group(1);
            String line = "\"" + group + " \"\n";
            result.add(line);
        }

        int lastElement = result.size() - 1;
        result.set(lastElement, result.get(lastElement)
                .replace(" \"\n", "\"\n"));
        return StringUtils.join(result, "");
    }
}
