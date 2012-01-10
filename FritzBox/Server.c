#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <netdb.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/fcntl.h>
#include <unistd.h>
#include <sys/signal.h>
#include <arpa/inet.h>
#include <string.h>
#include <errno.h>
#include <dirent.h>
#include <sys/stat.h>
#include <sys/statvfs.h>
#include <time.h>



#define true 1
#define false 0
#define bufferSize 100
#define maxPathSize 100
#define SAVE_FILES 1
#define RM_FILES 2
#define BEGIN_DATA 3
#define BEGIN_RM 4
#define STRING 2
#define ERROR 3
#define INTEGER 0
#define GET_DISK_SPACE 5

const int OK = 1;

char buffer[bufferSize];

typedef struct {
	char fileName[30];
	int fileLength;
	char fullName[100]; /*path and name*/
} MyFile;



void *receive(int cd) {	
	int type = INTEGER;
	recv(cd, &type, sizeof(int), 0);
	printf("type %d\n", type);
	if ((type & STRING) == STRING) {
		int length = 0;
		recv(cd, &length, sizeof(int), 0);
		printf("string length %d\n", length);
		if (length > 200 || length == 0) return 0;
		void *mem = malloc(length + 1);
		if (mem == 0) {
			return 0;		
		}
		memset(mem, 0, length + 1);
		recv(cd, mem, length, 0);
		int i = 0;
		printf("string: %s\n", (char *)mem);		
		if ((type & ERROR) == ERROR) {
			printf("%s\n", (char *)mem);
			free(mem);
			return (void *)-1;		
		} else {
			return mem;		
		}
	}  else {
		void *mem = malloc(sizeof(int));
		if (mem == 0) {
			return 0;		
		}
		memset(mem, 0, sizeof(int));
		printf("integer: ");
		recv(cd, mem, sizeof(int), 0);
		printf("%i\n", *(int*)mem);
		return mem;
	}
}

void initMyFile(MyFile *file) {		
	memset(file, 0, sizeof(MyFile));
}

void sendNotSynced(int cd, void const *ptr, int len, int type) {
	int integer = 0;
	int _type = htonl(type);	
	send(cd, &_type, sizeof(int), 0);
	if ((type & STRING) == STRING) {
		int _len = htonl(len);
		send(cd, &_len, sizeof(int), 0);	
	} else {
		if (type == INTEGER) {
			integer = htonl(*(int *)ptr);
			ptr = &integer;
		}	
	}
	send(cd, ptr, len, 0);	
}

void errorHandle(char *msg) {
	printf("Error: %s\n", msg);
}


void errorHandleSend(int clientdescriptor, char *msg) {
	sendNotSynced(clientdescriptor, msg, strlen(msg), ERROR);
	errorHandle(msg);
}


void saveFiles(int clientdescriptor, char *usbPath) {
	char *errorStr = 0;
	int *iPtr = (int *)receive(clientdescriptor);
	if (iPtr == 0) {
		errorHandleSend(clientdescriptor, "pathscount ptr is null\0");	
		return;
	} else {
		char *msg = "pathscount is ok\0";
		sendNotSynced(clientdescriptor, msg, strlen(msg), STRING);	
	}
	int pathsCount = *iPtr;
	free(iPtr);
	int h = 0;	
	for (; h < pathsCount; h++) {
		iPtr = (int *)receive(clientdescriptor);
		if (iPtr == 0) {
			errorHandleSend(clientdescriptor, "filescount ptr is null\0");
			return;
		} else {
			char *msg = "filescount ok\0";
			sendNotSynced(clientdescriptor, msg, strlen(msg), STRING);	
		}
		int filesCount = *iPtr;
		free(iPtr);		
		MyFile files[filesCount];	
		int i = 0;		
		for (;i < filesCount; i++) {
			MyFile *file = &files[i];
			initMyFile(file);
			strcpy(file->fullName, usbPath);
			char *cPtr = (char *)receive(clientdescriptor);
			if (cPtr == 0) {
				errorHandleSend(clientdescriptor, "filename ptr is null\0");
				return;		
			} else {
				if (cPtr == (void*)-1) {
					sendNotSynced(clientdescriptor, "ok\0", 2, STRING);
					continue;				
				}
				char *msg = "filename ptr is ok\0";
				sendNotSynced(clientdescriptor, msg, strlen(msg), STRING);
			}
			strcpy(file->fileName, cPtr);
			free(cPtr);
			strcat(file->fullName, file->fileName);
			printf("received file name: %s\n", file->fileName);
			iPtr = (int *)receive(clientdescriptor);
			if (iPtr == 0) {
				errorHandleSend(clientdescriptor, "filelength ptr is null\0");
				return;		
			} else {
				if (iPtr == (void*)-1) {
					sendNotSynced(clientdescriptor, "ok\0", 2, STRING);
					return;
				} else {
					char *msg = "filelength ptr is ok\0";
					sendNotSynced(clientdescriptor, msg, strlen(msg), STRING);
				}
			}
			file->fileLength = *iPtr;
			free(iPtr);
			printf("received file length: %d\n", file->fileLength);  

			FILE *pFile = fopen(file->fullName, "w+b");
			if (pFile == 0) {
				errorHandleSend(clientdescriptor, strerror(errno));
			} else {
				iPtr = (int *)receive(clientdescriptor);
				if (iPtr == 0) {
					errorHandleSend(clientdescriptor, "begin_data ptr is null\0");
					fclose(pFile);
					return;
				} else {
					if (iPtr == (void*)-1) {
						sendNotSynced(clientdescriptor, "ok\0", 2, STRING);
						return;						
					} else {
						char *msg = "begin_data ptr is ok\0";
						sendNotSynced(clientdescriptor, msg, strlen(msg), STRING);
					}
				}
				int begin_data = *iPtr;
				free(iPtr);
				if (begin_data == BEGIN_DATA) {
					printf("writing into: %s\n", file->fullName);
					int bytesRead = 0;

					while (bytesRead < file->fileLength) {
						int remainingBytesCount = file->fileLength - bytesRead;
						int actualBufferSize = 0;					
						if (remainingBytesCount >= bufferSize) {
							actualBufferSize = bufferSize;
						} else {
							memset(buffer, 0, bufferSize);
							actualBufferSize = remainingBytesCount;
						}	
						int bytesRecv = recv(clientdescriptor, buffer, actualBufferSize, 0);
						//printf("bytes received: %d\n", bytesRecv);
						fwrite(buffer, 1, bytesRecv, pFile);				
						bytesRead += bytesRecv;			
					}
					fputs("\0", pFile);
					printf("finished writing %s\n", file->fullName);
					sendNotSynced(clientdescriptor, "done\0", 5, STRING); 
				} else {
					errorHandleSend(clientdescriptor, "wrong integer for begin_data\0");
					fclose(pFile);
					return;
				}
				fclose(pFile);
			}

		}
	}	
}

void rmFiles(int clientdescriptor, char *usbPath) {
	DIR *mydir = opendir(usbPath);
	if (mydir == 0) {
		errorHandleSend(clientdescriptor, strerror(errno));
	} else {
		char *msg = "opened folder\0";
		sendNotSynced(clientdescriptor, msg, strlen(msg), STRING);
		int begin_rm = 0;
		int *iPtr = (int *)receive(clientdescriptor);
				
		if (iPtr != 0) {
			char *msg = "begin_rm received\0";
			sendNotSynced(clientdescriptor, msg, strlen(msg), STRING);
			int begin_rm = *iPtr;
			free(iPtr);
			if (begin_rm == BEGIN_RM) {
				struct dirent *entry = NULL;
				struct stat statbuf;
				   
				while((entry = readdir(mydir))) {
					char tmp[strlen(entry->d_name) + strlen(usbPath)];

					strcpy(tmp, usbPath);
					strcat(tmp, entry->d_name);
					stat(tmp, &statbuf);
	   				if (S_ISREG(statbuf.st_mode)) {
						if (remove(tmp) == -1) {
							printf("%s\n", strerror(errno));
				  			errorHandleSend(clientdescriptor, strerror(errno));
							closedir(mydir);					
							return;
						}
					}
				}
				printf("deleted\n");
				sendNotSynced(clientdescriptor, "deleted\0", 8, STRING);
			} else {
				errorHandleSend(clientdescriptor, "wrong integer for begin_rm\0");
			}
		} else {
			errorHandleSend(clientdescriptor, "begin_rm ptr is null\0");
		}
		closedir(mydir);
	}
}

void getDiskSpace(int cd, char *usbPath) {

   struct statvfs buf;

   if (!statvfs(usbPath, &buf)) {
      unsigned long blksize, freeblks;
      double free;
      blksize = buf.f_bsize;
      freeblks = buf.f_bfree;

      free = (freeblks/4096)*(blksize/4096);
      free = free / (1024*1024);
      free = free * (4096*4096);
      int i = (int)free;
      sendNotSynced(cd, &i, sizeof(int), INTEGER);
   }
   else {
      errorHandleSend(cd, "Failed to stat\0");
   }

}



int main(int argc, char **argv) {
    char *usbPath;
    if (argc == 2) {
	usbPath = argv[1];
	char c;	
	char last_c;
	while (c = *usbPath) {
		last_c = c;
		usbPath++;
	}
	if (last_c != '/') {
		printf("Path must end with \'/\'\n");
		return 1;	
	}
	usbPath = argv[1];
    } else {
	printf("usage: server [PATH]\n");
	return 1;
    }
    struct sockaddr_in mySocket;
    struct sockaddr_in clientSocket;
    int descriptor, clientdescriptor, clientLength = sizeof(clientSocket);

    descriptor = socket(AF_INET, SOCK_STREAM, 0);

    if (descriptor == -1)
    {
        perror("socket() failed\n");
        exit(EXIT_FAILURE);
    }

    mySocket.sin_family = AF_INET;
    mySocket.sin_addr.s_addr = INADDR_ANY;
    mySocket.sin_port = htons(8081);
	
    if ((bind(descriptor, (struct sockaddr *)&mySocket, sizeof(mySocket))) == -1)
    {
        perror("bind() error\n");
        exit(EXIT_FAILURE);
    }
    if ((listen(descriptor, 5)) == -1)
    {
        exit(EXIT_FAILURE);
    }

    printf("gestartet\n");

    do
    {
        clientdescriptor = accept(descriptor, (struct sockaddr *)&clientSocket, &clientLength);
        if (clientdescriptor == -1) {
           errorHandle("accept() failed\0");
        } else {
            if (fork() == 0) {
		time_t rawtime;
  		time (&rawtime);
		printf("incoming connection from %s at %s", inet_ntoa(clientSocket.sin_addr), ctime(&rawtime));
		char *password = (char *)receive(clientdescriptor);
		char pw[16];
		FILE *pFile = fopen("password", "r");
		if (pFile == 0) {
			errorHandleSend(clientdescriptor, "password file not found\0");
		} else {
			fgets(pw, 16, pFile);
			if (pFile != 0 && password != 0 && strcmp(pw, password) == 0) {
				printf("authorized with password: %s\n", password); 
				char *msg = "authorized\0";
				sendNotSynced(clientdescriptor, msg, strlen(msg), STRING);
				int *iPtr = (int *)receive(clientdescriptor);
				if (iPtr != 0) {
					char *msg = "command received\0";
					sendNotSynced(clientdescriptor, msg, strlen(msg), STRING);
					int command = *iPtr;
					free(iPtr);
					switch (command) {
						case SAVE_FILES: saveFiles(clientdescriptor, usbPath); break;
						case RM_FILES: rmFiles(clientdescriptor, usbPath); break;
						case GET_DISK_SPACE: getDiskSpace(clientdescriptor, usbPath); break;
						default: break;
					}
				} else {
					errorHandleSend(clientdescriptor, "command ptr is null\0");		
				}
			} else {
				errorHandleSend(clientdescriptor, "wrong password\0");
			}
		}
		close(clientdescriptor);
                exit(EXIT_SUCCESS);
            }
        }
    } while (true);

    return EXIT_SUCCESS;
}

