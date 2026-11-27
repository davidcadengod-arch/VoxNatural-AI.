#include <jni.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <lame.h>
static uint32_t le(const unsigned char *p){return (uint32_t)p[0]|((uint32_t)p[1]<<8)|((uint32_t)p[2]<<16)|((uint32_t)p[3]<<24);}
JNIEXPORT jint JNICALL Java_com_voxnatural_app_Mp3Encoder_encodeNative(JNIEnv *env,jobject self,jstring input,jstring output){
 const char *in=(*env)->GetStringUTFChars(env,input,0),*out=(*env)->GetStringUTFChars(env,output,0);
 FILE *f=NULL,*g=NULL; lame_t l=NULL; int rc=-1;
 unsigned char h[44], bytes[8192], mp3[16384]; short pcm[4096];
 if(!in||!out)goto end;
 f=fopen(in,"rb");if(!f)goto end;
 if(fread(h,1,44,f)!=44||memcmp(h,"RIFF",4)||memcmp(h+8,"WAVEfmt ",8)||memcmp(h+36,"data",4)||le(h+16)!=16||h[20]!=1||h[21]!=0||h[34]!=16||h[35]!=0)goto end;
 int channels=h[22]+256*h[23];if(channels!=1&&channels!=2)goto end;
 uint32_t left=le(h+40);if(left%(2*channels))goto end;
 l=lame_init();if(!l)goto end;
 lame_set_in_samplerate(l,(int)le(h+24));lame_set_num_channels(l,channels);lame_set_brate(l,128);lame_set_quality(l,3);lame_set_bWriteVbrTag(l,0);
 if(lame_init_params(l)<0)goto end;
 g=fopen(out,"wb");if(!g)goto end;
 while(left){
  size_t n=left>sizeof(bytes)?sizeof(bytes):left;
  if(fread(bytes,1,n,f)!=n)goto end;
  for(size_t i=0;i<n/2;i++)pcm[i]=(int16_t)(bytes[2*i]|((uint16_t)bytes[2*i+1]<<8));
  int k=channels==1?lame_encode_buffer(l,pcm,pcm,n/2,mp3,sizeof(mp3)):lame_encode_buffer_interleaved(l,pcm,n/4,mp3,sizeof(mp3));
  if(k<0||fwrite(mp3,1,k,g)!=(size_t)k)goto end;
  left-=n;
 }
 int k=lame_encode_flush(l,mp3,sizeof(mp3));if(k<0||fwrite(mp3,1,k,g)!=(size_t)k)goto end;
 rc=0;
 end: if(g&&fclose(g)!=0)rc=-1;if(f)fclose(f);if(l)lame_close(l);
 if(in)(*env)->ReleaseStringUTFChars(env,input,in);if(out)(*env)->ReleaseStringUTFChars(env,output,out);return rc;
}
