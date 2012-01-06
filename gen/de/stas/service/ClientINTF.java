/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/bline/workspace/FileSender/src/de/stas/service/ClientINTF.aidl
 */
package de.stas.service;
public interface ClientINTF extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements de.stas.service.ClientINTF
{
private static final java.lang.String DESCRIPTOR = "de.stas.service.ClientINTF";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an de.stas.service.ClientINTF interface,
 * generating a proxy if needed.
 */
public static de.stas.service.ClientINTF asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof de.stas.service.ClientINTF))) {
return ((de.stas.service.ClientINTF)iin);
}
return new de.stas.service.ClientINTF.Stub.Proxy(obj);
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
case TRANSACTION_newMessages:
{
data.enforceInterface(DESCRIPTOR);
this.newMessages();
reply.writeNoException();
return true;
}
case TRANSACTION_newLine:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.newLine(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_error:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.error(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_progress:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.progress(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements de.stas.service.ClientINTF
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
public void newMessages() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_newMessages, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void newLine(java.lang.String line) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(line);
mRemote.transact(Stub.TRANSACTION_newLine, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void error(java.lang.String errMsg) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(errMsg);
mRemote.transact(Stub.TRANSACTION_error, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void progress(java.lang.String i) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(i);
mRemote.transact(Stub.TRANSACTION_progress, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_newMessages = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_newLine = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_error = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_progress = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
}
public void newMessages() throws android.os.RemoteException;
public void newLine(java.lang.String line) throws android.os.RemoteException;
public void error(java.lang.String errMsg) throws android.os.RemoteException;
public void progress(java.lang.String i) throws android.os.RemoteException;
}
