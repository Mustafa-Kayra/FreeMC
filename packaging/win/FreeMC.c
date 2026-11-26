/* Windows baslatici: FreeMC.exe, yanindaki gomulu jre ile freemc.jar'i acar. */
#ifndef UNICODE
#define UNICODE
#endif
#ifndef _UNICODE
#define _UNICODE
#endif
#include <windows.h>
#include <wchar.h>

static void fail(const wchar_t *message) {
    MessageBoxW(NULL, message, L"FreeMC", MB_ICONERROR | MB_OK);
    ExitProcess(1);
}

static int fileExists(const wchar_t *path) {
    return GetFileAttributesW(path) != INVALID_FILE_ATTRIBUTES;
}

int WINAPI wWinMain(HINSTANCE instance, HINSTANCE previous, PWSTR args, int show) {
    (void) instance;
    (void) previous;
    (void) show;

    wchar_t home[32768];
    if (!GetModuleFileNameW(NULL, home, 32768)) {
        fail(L"FreeMC klasoru bulunamadi.");
    }
    wchar_t *slash = wcsrchr(home, L'\\');
    if (slash == NULL) {
        fail(L"FreeMC klasoru bulunamadi.");
    }
    *slash = L'\0';

    wchar_t javaw[32768];
    wchar_t jar[32768];
    _snwprintf(javaw, 32768, L"%s\\jre\\bin\\javaw.exe", home);
    _snwprintf(jar, 32768, L"%s\\freemc.jar", home);

    if (!fileExists(javaw)) {
        fail(L"jre klasoru eksik.\n\nFreeMC kurulumunu tekrar calistir.");
    }
    if (!fileExists(jar)) {
        fail(L"freemc.jar bulunamadi.\n\nFreeMC kurulumunu tekrar calistir.");
    }

    wchar_t command[65536];
    _snwprintf(command, 65536, L"\"%s\" -jar \"%s\" %s", javaw, jar, args != NULL ? args : L"");

    STARTUPINFOW startup;
    PROCESS_INFORMATION process;
    ZeroMemory(&startup, sizeof startup);
    startup.cb = sizeof startup;
    ZeroMemory(&process, sizeof process);

    if (!CreateProcessW(NULL, command, NULL, NULL, FALSE, 0, NULL, home, &startup, &process)) {
        wchar_t message[512];
        _snwprintf(message, 512, L"Java baslatilamadi (hata %lu).", GetLastError());
        fail(message);
    }

    CloseHandle(process.hThread);
    CloseHandle(process.hProcess);
    return 0;
}
