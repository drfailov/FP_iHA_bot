package com.fsoft.vktest;

/**
 * список игнорируемых лалок)
 * Created by Dr. Failov on 01.01.2015.
 */
public class TeacherListFragment extends AllowListFragment {
    @Override
    String getFragmentName() {
        return "Учителя";
    }

    @Override
    UserList getUserList() {
        return applicationManager.messageProcessor.teachId;
    }

    @Override
    String getListDescription() {
        return "Общаясь с учителями, бот будет пополнять свою базу ответов, записывая каждое сообщение учителя как ответ на предыдущую реплику бота.";
    }
}
