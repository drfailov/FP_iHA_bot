package com.fsoft.ihabot.answer;

import androidx.annotation.NonNull;

import com.fsoft.ihabot.communucation.tg.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Locale;

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

    public AnswerElement() {
    }
    public AnswerElement(JSONObject jsonObject)throws JSONException, ParseException {
        fromJson(jsonObject);
    }

    public long getId() {
        return id;
    }

    public Message getQuestionMessage() {
        return questionMessage;
    }

    public Message getAnswerMessage() {
        return answerMessage;
    }

    public boolean hasAnswer(){
        return answerMessage != null;
    }

    public void setId(long id) {
        this.id = id;
    }

    /**
     * Используется для быстрого подбора унимального максимального ID.
     * Эта функция задаёт ID на единицу больше того что принят, при условии,
     * что принятый ID больше или такой же как текущий.
     * @param id ID, больше которого надо сделать ID этого ответа
     */
    public void setIdBiggerThan(long id) {
        if(id >= this.id)
            this.id = id+1;
    }

    public void setQuestionMessage(Message questionMessage) {
        this.questionMessage = questionMessage;
    }

    public void setAnswerMessage(Message answerMessage) {
        this.answerMessage = answerMessage;
    }

    public int getTimesUsed() {
        return timesUsed;
    }

    public void incrementTimesUsed() {
        this.timesUsed++;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("timesUsed", timesUsed);
        if(questionMessage != null)
            jsonObject.put("questionMessage", questionMessage.toJson());
        if(answerMessage != null)
            jsonObject.put("answerMessage", answerMessage.toJson());
        return jsonObject;
    }

    private void fromJson(JSONObject jsonObject)throws JSONException, ParseException {
        if(jsonObject.has("id"))
            id = jsonObject.getLong("id");
        if(jsonObject.has("timesUsed"))
            timesUsed = jsonObject.getInt("timesUsed");

        if(jsonObject.has("questionMessage"))
            questionMessage = new Message(jsonObject.getJSONObject("questionMessage"));

        if(jsonObject.has("answerMessage"))
            answerMessage = new Message(jsonObject.getJSONObject("answerMessage"));
    }

    public String toStringWithID() {
        if(getQuestionMessage() == null && getAnswerMessage() == null){
            return String.format(Locale.US, "ID%d NULL -> NULL", getId());
        }
        if(getQuestionMessage() == null){
            return String.format(Locale.US, "ID%d NULL -> %s",
                    getId(),
                    getAnswerMessage().toString().replace("\n", ""));
        }
        if(getAnswerMessage() == null){
            return String.format(Locale.US, "ID%d %s -> NULL",
                    getId(),
                    getQuestionMessage().toString().replace("\n", ""));
        }
        return String.format(Locale.US, "ID%d %s -> %s",
                getId(),
                getQuestionMessage().toString().replace("\n", ""),
                getAnswerMessage().toString().replace("\n", ""));

    }
    @NonNull
    @Override
    public String toString() {
        if(getQuestionMessage() == null && getAnswerMessage() == null){
            return "NULL -> NULL";
        }
        if(getQuestionMessage() == null){
            return String.format(Locale.US, "NULL -> %s",
                    getAnswerMessage().toString().replace("\n", ""));
        }
        if(getAnswerMessage() == null){
            return String.format(Locale.US, "%s -> NULL",
                    getQuestionMessage().toString().replace("\n", ""));
        }
        return String.format(Locale.US, "%s -> %s",
                getQuestionMessage().toString().replace("\n", ""),
                getAnswerMessage().toString().replace("\n", ""));

    }
}
