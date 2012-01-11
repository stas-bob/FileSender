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
#define bufferSize 1024
#define maxPathSize 100
#define SAVE_FILES 1
#define RM_FILES 2
#define BEGIN_DATA 3
#define BEGIN_RM 4
#define STRING 2
#define ERROR 3
#define INTEGER 0
#define GET_DISK_SPACE 5
#define SKIP 14

const int OK = 1;

char buffer[bufferSize];

typedef struct {
	char fileName[30];
	int fileLength;
	char fullName[100]; /*path and name*/
} MyFile;



void *receive(int cd, int *pType, void *vPtr) {
	free(vPtr);	
	recv(cd, pType, sizeof(int), 0);
	if ((*pType & STRING) == STRING) {
		int length = 0;
		recv(cd, &length, sizeof(int), 0);
		if (length > 200 || length == 0) {
			printf("String is too long or empty\n");
			vPtr = 0;
		} else {
			vPtr = malloc(length + 1);
			if (vPtr != 0) {
				memset(vPtr, 0, length + 1);
				recv(cd, vPtr, length, 0);
			}
		}
	}  else {
		vPtr = malloc(sizeof(int));
		if (vPtr != 0) {
			memset(vPtr, 0, sizeof(int));
			recv(cd, vPtr, sizeof(int), 0);
		}
	}
	return vPtr;
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


void errorHandleSend(int clientdescriptor, char *msg) {
	sendNotSynced(clientdescriptor, msg, strlen(msg), ERROR);
	printf("Error: %s\n", msg);
}


void *saveFiles(int clientdescriptor, char *usbPath, void *vPtr) {	
	char *errorStr = 0;
	int type;
	vPtr = receive(clientdescriptor, &type, vPtr);
	if (vPtr == 0) {
		errorHandleSend(clientdescriptor, "pathscount ptr is null\0");	
	} else {
		if (type == ERROR) {
			printf("client error received: %s\n", (char *)vPtr);
		} else {
			char *msg = "pathscount is ok\0";
			sendNotSynced(clientdescriptor, msg, strlen(msg), STRING);
			int pathsCount = *((int *)vPtr);
			int h = 0;	
			for (; h < pathsCount; h++) {
				vPtr = receive(clientdescriptor, &type, vPtr);
				if (vPtr == 0) {
					errorHandleSend(clientdescriptor, "filescount ptr is null\0");
					return vPtr;
				} else {
					if (type == ERROR) {
						printf("client error received: %s\n", (char *)vPtr);
						return vPtr;
					} else {
						char *msg = "filescount is ok\0";
						sendNotSynced(clientdescriptor, msg, strlen(msg), STRING);
						int filesCount = *((int *)vPtr);	
						MyFile files[filesCount];	
						int i = 0;		
						for (;i < filesCount; i++) {
							MyFile *file = &files[i];
							initMyFile(file);
							strcpy(file->fullName, usbPath);
							vPtr = receive(clientdescriptor, &type, vPtr);
							if (vPtr == 0) {
								errorHandleSend(clientdescriptor, "filename ptr is null\0");
								return vPtr;	
							} else {
								if (type == ERROR) {
									printf("client error received: %s\n", (char *)vPtr);
									return vPtr;			
								} else {
									char *msg = "filename ptr is ok\0";
									sendNotSynced(clientdescriptor, msg, strlen(msg), STRING);
									strcpy(file->fileName, (char *)vPtr);
									strcat(file->fullName, file->fileName);
									printf("received file name: %s\n", file->fileName);
									vPtr = receive(clientdescriptor, &type, vPtr);
									if (vPtr == 0) {
										errorHandleSend(clientdescriptor, "filelength ptr is null\0");
										return vPtr;	
									} else {
										if (type == ERROR) {
											printf("client error received: %s\n", (char *)vPtr);
											return vPtr;
										} else {
											if (type == SKIP) {
												printf("skipping file %s\n", file->fullName);
											} else {
												char *msg = "filelength ptr is ok\0";
												sendNotSynced(clientdescriptor, msg, strlen(msg), STRING);
												file->fileLength = *((int *)vPtr);
												printf("received file length: %d\n", file->fileLength);  

												FILE *pFile = fopen(file->fullName, "w+b");
												if (pFile == 0) {
													errorHandleSend(clientdescriptor, strerror(errno));
													return vPtr;
												} else {
													vPtr = receive(clientdescriptor, &type, vPtr);
													if (vPtr == 0) {
														errorHandleSend(clientdescriptor, "begin_data ptr is null\0");
														fclose(pFile);
														return vPtr;
													} else {
														if (type == ERROR) {
															printf("client error received: %s\n", (char *)vPtr);
															return vPtr;						
														} else {
															char *msg = "begin_data ptr is ok\0";
															sendNotSynced(clientdescriptor, msg, strlen(msg), STRING);
															int begin_data = *((int *)vPtr);
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
																	fwrite(buffer, 1, bytesRecv, pFile);				
																	bytesRead += bytesRecv;			
																}
																fputs("\0", pFile);
																printf("finished writing %s\n", file->fullName);
																sendNotSynced(clientdescriptor, "done\0", 5, STRING); 
															} else {
																errorHandleSend(clientdescriptor, "wrong integer for begin_data\0");
																fclose(pFile);
																return vPtr;
															}
															fclose(pFile);
														}
													}
												}
											}
										}
									}
								}	
							}		
						} //end for
					}	
				}
			} // end for
		}
	}
	return vPtr;	
}

void *rmFiles(int clientdescriptor, char *usbPath, void *vPtr) {
	DIR *mydir = opendir(usbPath);
	if (mydir == 0) {
		errorHandleSend(clientdescriptor, strerror(errno));
	} else {
		char *msg = "opened folder\0";
		sendNotSynced(clientdescriptor, msg, strlen(msg), STRING);
		int begin_rm = 0;
		int type;
		vPtr = receive(clientdescriptor, &type, vPtr);
				
		if (vPtr == 0) {
			errorHandleSend(clientdescriptor, "begin_rm ptr is null\0");	
		} else {
			if (type == ERROR) {
				printf("client error received: %s\n", (char *)vPtr);	
			} else {
				char *msg = "begin_rm received\0";
				sendNotSynced(clientdescriptor, msg, strlen(msg), STRING);
				int begin_rm = *((int *)vPtr);
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
			}
		}
		closedir(mydir);	
	}
	return vPtr;
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
	
    void *vPtr = 0;
    do {
        clientdescriptor = accept(descriptor, (struct sockaddr *)&clientSocket, &clientLength);
        if (clientdescriptor == -1) {
           printf("accept() failed\n");
        } else {
            if (fork() == 0) {
		time_t rawtime;
  		time (&rawtime);
		printf("incoming connection from %s at %s", inet_ntoa(clientSocket.sin_addr), ctime(&rawtime));
		int type;		
		vPtr = receive(clientdescriptor, &type, vPtr);
		if (vPtr == 0) {
			errorHandleSend(clientdescriptor, "password ptr is null\0");	
		} else {
			if (type == ERROR) {
				printf("client error received: %s\n", (char *)vPtr);		
			} else {
				char pw[16];
				char *password = (char *)vPtr;
				FILE *pFile = fopen("password", "r");
				if (pFile == 0) {
					errorHandleSend(clientdescriptor, "password file not found\0");
				} else {
					fgets(pw, 16, pFile);
					if (strcmp(pw, password) == 0) {
						printf("authorized with password: %s\n", password); 
						char *msg = "authorized\0";
						sendNotSynced(clientdescriptor, msg, strlen(msg), STRING);
						vPtr = receive(clientdescriptor, &type, vPtr);
						if (vPtr == 0) {
							errorHandleSend(clientdescriptor, "command ptr is null\0");						
						} else {
							if (type == ERROR) {
								printf("client error received: %s\n", (char *)vPtr);
							} else {
								char *msg = "command received\0";
								sendNotSynced(clientdescriptor, msg, strlen(msg), STRING);
								int command = *((int *)vPtr);
								switch (command) {
									case SAVE_FILES: vPtr = saveFiles(clientdescriptor, usbPath, vPtr); break;
									case RM_FILES: vPtr = rmFiles(clientdescriptor, usbPath, vPtr); break;
									case GET_DISK_SPACE: getDiskSpace(clientdescriptor, usbPath); break;
									default: break;
								}
							}
										
						}
					} else {
						errorHandleSend(clientdescriptor, "wrong password\0");
					}
					fclose(pFile);
				}
			}		
		}
		free(vPtr);
	    	close(clientdescriptor);
	    	exit(EXIT_SUCCESS);
            }

        }
    } while (true);

    return EXIT_SUCCESS;
}

