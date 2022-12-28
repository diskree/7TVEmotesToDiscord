#include <jni.h>
#include <stdlib.h>
#include <string.h>

int main(int argc, char **argv);

JNIEXPORT jint JNICALL
Java_com_diskree_emotes2discord_MainActivity_runCommand(
        JNIEnv *env, jobject object,
        jobjectArray stringArray
) {
    jstring *tempArray = NULL;
    int argumentCount = 1;
    char **argv = NULL;

    if (stringArray) {
        int programArgumentCount = (*env)->GetArrayLength(env, stringArray);
        argumentCount = programArgumentCount + 1;

        tempArray = (jstring *) malloc(sizeof(jstring) * programArgumentCount);
    }

    argv = (char **) malloc(sizeof(char *) * (argumentCount));
    argv[0] = (char *) malloc(sizeof(char) * (strlen("gifsicle") + 1));
    strcpy(argv[0], "gifsicle");

    if (stringArray) {
        for (int i = 0; i < (argumentCount - 1); i++) {
            tempArray[i] = (jstring) (*env)->GetObjectArrayElement(env, stringArray, i);
            if (tempArray[i] != NULL) {
                argv[i + 1] = (char *) (*env)->GetStringUTFChars(env, tempArray[i], 0);
            }
        }
    }

    int returnCode = main(argumentCount, argv);

    if (tempArray) {
        for (int i = 0; i < (argumentCount - 1); i++) {
            (*env)->ReleaseStringUTFChars(env, tempArray[i], argv[i + 1]);
        }
        free(tempArray);
    }
    free(argv[0]);
    free(argv);

    return returnCode;
}
