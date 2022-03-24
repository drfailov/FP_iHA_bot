package com.fsoft.ihabot.answer;

/* * Какая инфа про ответ должна хранится:
 * - ID ответа (id, long. В случае коллизий генерировать новые)
 * - Message вопроса (текст вопроса, автор вопроса, дата вопроса)
 * - Message ответа (текст ответа, автор ответа, дата ответа)
* */
public class AnswerElement {
    private long id = 0; //В случае коллизий генерировать новые)
    private Message questionMessage = null;
    private Message answerMessage = null;
    private int timesUsed = 0; //Количество раз, сколько раз бот использовал этот ответ


}
