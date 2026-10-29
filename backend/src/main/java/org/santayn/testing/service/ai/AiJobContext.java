package org.santayn.testing.service.ai;

/** Связь обработчика с задачей: прогресс и учёт расхода. */
public interface AiJobContext {

    void progress(String text);

    void addUsage(AiChatClient.AiChatResult result);

    void addOcrPages(int pages);

    void addGeneratedImages(int images);
}
