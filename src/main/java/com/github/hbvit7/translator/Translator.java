package com.github.hbvit7.translator;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Translator {

    public static final String MSGID_BEGIN = "msgid \"";
    public static final String MSGSTR_BEGIN = "msgstr \"";
    public static final String QUOTATION_MARK = "\"";
    public static final String MSGID_MULTILINE = MSGID_BEGIN + QUOTATION_MARK;
    public static final int MAX_LINE_LENGHT = 76;

    private GoogleApiWithoutApiKey googleTranslateWithoutApiKey = new GoogleApiWithoutApiKey();
    private MaxLengthStringSplitter maxLengthStringSplitter = new MaxLengthStringSplitter(MAX_LINE_LENGHT);

    public static void main(String[] args) {
        Translator translator = new Translator();
        translator.start();
    }

    private void start() {
        String name = "/basic.po";
        URL resource = getClass().getResource("/basic.po");
        if (resource == null) {
            throw new RuntimeException("Cannont find resource: " + name);
        }
        String allText = readFile(resource.getFile());

        String[] records = allText.split("\n\n");
        List<String> translatedRecords = new ArrayList<>();
        int countRecosds = records.length;
        int current = 0;
        for (String record : records) {
            String translatedRecord = processRecord(record);
            translatedRecords.add(translatedRecord);
            System.out.println(++current + "/" + countRecosds);
//            System.out.println(translatedRecord);
        }

        try {
            Files.write(Paths.get("basic_ru.po"), translatedRecords);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String processRecord(String record) {
        StringBuilder builder = new StringBuilder();
        String[] recordLines = record.split("\n");
        int i = 0;
        while (i < recordLines.length) {
            String recordLine = recordLines[i].trim();
            String translated = "";
            if (isMsgid(recordLine)) {
                StringBuilder forTranslationBuilder = new StringBuilder();
//                System.out.println(recordLine);
                builder.append(recordLine).append("\n");
                if (isMultilineMsgid(recordLine)) {
                    i++;
                    String nextRecordLine = recordLines[i].trim();
                    while (isMsgidNext(nextRecordLine)) {
//                    System.out.println(nextRecordLine);
                        builder.append(nextRecordLine).append("\n");
                        forTranslationBuilder
                                .append(nextRecordLine, 1, nextRecordLine.length() - 1);
                        i++;
                        nextRecordLine = recordLines[i].trim();
                    }
                } else {
                    forTranslationBuilder
                            .append(recordLine, MSGSTR_BEGIN.length() - 1, recordLine.length() - 1);
                }

                String forTranslation = forTranslationBuilder.toString();
                if (!StringUtils.isBlank(forTranslation)) {
                    try {
                        translated = googleTranslateWithoutApiKey.translate("en", "ru", forTranslation);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                if (isMultilineMsgid(recordLine)) {
                    String splitted = maxLengthStringSplitter.split(translated);
//                    System.out.println(MSGSTR_BEGIN + QUOTATION_MARK);
                    builder.append(MSGSTR_BEGIN)
                            .append(QUOTATION_MARK)
                            .append("\n")
                            .append(splitted);
                } else {
//                    System.out.println(MSGSTR_BEGIN + translated + QUOTATION_MARK);
                    builder.append(MSGSTR_BEGIN)
                            .append(translated)
                            .append(QUOTATION_MARK)
                            .append("\n");
                }
//                System.out.println();

            } else if (!isMsgstr(recordLine)) {
//                System.out.println(recordLine);
                builder.append(recordLine).append("\n");
            }

            i++;
        }
        return builder.toString();
    }

    private boolean isMsgidNext(String nextRecordLine) {
        return nextRecordLine.startsWith(QUOTATION_MARK) && nextRecordLine.endsWith(QUOTATION_MARK);
    }

    private boolean isMsgstr(String recordLine) {
        return recordLine.startsWith(MSGSTR_BEGIN);
    }

    private static boolean isMultilineMsgid(String recordLine) {
        return recordLine.startsWith(MSGID_MULTILINE);
    }

    private static boolean isMsgid(String recordLine) {
        return recordLine.startsWith(MSGID_BEGIN);
    }

    private String readFile(String filename) {
        String sEncoding = "UTF-8";
        String allText = "";
        try (FileInputStream fstream = new FileInputStream(filename);
             DataInputStream in = new DataInputStream(fstream);
             BufferedReader br = new BufferedReader(new InputStreamReader(in, sEncoding));) {

            String strLine;

            while ((strLine = br.readLine()) != null) {
                allText += strLine + "\n";
//                System.out.println(strLine);
            }

            allText = allText.replaceFirst("\uFEFF", "")
                    .replaceFirst("\uEFBBBF", "");
        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
        return allText;
    }
}
