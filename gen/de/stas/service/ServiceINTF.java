/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/bline/workspace/FileSender/src/de/stas/service/ServiceINTF.aidl
 */
package de.stas.service;
public interface ServiceINTF extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements de.stas.service.ServiceINTF
{
private static final java.lang.String DESCRIPTOR = "de.stas.service.ServiceINTF";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an de.stas.service.ServiceINTF interface,
 * generating a proxy if needed.
 */
public static de.stas.service.ServiceINTF asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof de.stas.service.ServiceINTF))) {
return ((de.stas.service.ServiceINTF)iin);
}
return new de.stas.service.ServiceINTF.Stub.Proxy(obj);
}
public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_register:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
boolean _result = this.register(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_unregister:
{
data.enforceInterface(DESCRIPTOR);
this.unregister();
reply.writeNoException();
return true;
}
case TRANSACTION_getTimeTillNextScan:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getTimeTillNextScan();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_scanNow:
{
data.enforceInterface(DESCRIPTOR);
this.scanNow();
reply.writeNoException();
return true;
}
case TRANSACTION_deleteRemoteFiles:
{
data.enforceInterface(DESCRIPTOR);
this.deleteRemoteFiles();
reply.writeNoException();
return true;
}
case TRANSACTION_getRemoteFreeSpace:
{
data.enforceInterface(DESCRIPTOR);
this.getRemoteFreeSpace();
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements de.stas.service.ServiceINTF
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
public boolean register(java.lang.String appName) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(appName);
mRemote.transact(Stub.TRANSACTION_register, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public void unregister() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_unregister, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public int getTimeTillNextScan() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getTimeTillNextScan, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public void scanNow() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_scanNow, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void deleteRemoteFiles() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_deleteRemoteFiles, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void getRemoteFreeSpace() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getRemoteFreeSpace, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_register = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_unregister = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_getTimeTillNextScan = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_scanNow = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_deleteRemoteFiles = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_getRemoteFreeSpace = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
}
public boolean register(java.lang.String appName) throws android.os.RemoteException;
public void unregister() throws android.os.RemoteException;
public int getTimeTillNextScan() throws android.os.RemoteException;
public void scanNow() throws android.os.RemoteException;
public void deleteRemoteFiles() throws android.os.RemoteException;
public void getRemoteFreeSpace() throws android.os.RemoteException;
}
