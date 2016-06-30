package org.igarape.webrecorder.libs;

/**
 * Created by martelli on 6/17/16.
 */
public class LibYUV {

    /*
    Various malipulation routines for YUV images.
    Camera images are NV12. Out output is NV21.
     */

    public static void nv21tovn12(byte[] in, byte[] out, int w, int h) {

        for(int i=0; i<w*h; i++) {
            out[i] = in[i];
        }

        for(int i=0; i<w*h/4; i++) {
            out[w*h + 2*i] = in[w*h + 2*i + 1];
            out[w*h + 2*i+ 1] = in[w*h + 2*i];
        }

    }

    public static void transpose(byte[] in, byte[] out, int w, int h) {

        for(int x=0; x<w; x++) {
            for(int y=0; y<h; y++) {
                out[(x+1)*h-1-y] = in[y*w+x];
            }
        }

        for(int x=0; x<w/2; x++) {
            for(int y=0; y<h/2; y++) {
                out[w*h+(((x+1)*(h/2))-1-y)*2+1] = in[w*h+(y*w/2+x)*2];
                out[w*h+(((x+1)*(h/2))-1-y)*2] = in[w*h+(y*w/2+x)*2+1];
            }
        }
    }

    public static void transpose_bottom(byte[] in, byte[] out, int w, int h) {

        for(int x2=0; x2<w; x2++) {
            int x = w-1-x2;
            for(int y=0; y<h; y++) {
                int y2 = h-1-y;
                out[(x+1)*h-1-y] = in[y2*w+x2];
            }
        }

        for(int x2=0; x2<w/2; x2++) {
            int x = w/2-1-x2;
            for(int y=0; y<h/2; y++) {
                int y2 = h/2-1-y;
                out[w*h+(((x+1)*(h/2))-1-y)*2+1] = in[w*h+(y2*w/2+x2)*2];
                out[w*h+(((x+1)*(h/2))-1-y)*2] = in[w*h+(y2*w/2+x2)*2+1];
            }
        }
    }

    public static void transpose_flip_vert(byte[] in, byte[] out, int w, int h) {

        for (int y=0; y<h; y++) {
            for(int x=0; x<w; x++) {
                out[y*w+x] = in[(h-1-y)*w+(w-1-x)];
            }
        }

        for (int y=0; y<h/2; y++) {
            for(int x=0; x<w/2; x++) {
                out[w*h+(y*w/2+(w/2-1-x))*2+1] = in[w*h+((h/2-1-y)*w/2+x)*2];
                out[w*h+(y*w/2+(w/2-1-x))*2] = in[w*h+((h/2-1-y)*w/2+x)*2+1];
            }
        }
    }

    public static byte[] shrink(byte[] in, int w, int h, int scale) {

        byte[] out = new byte[in.length/scale/scale];

        for(int row=0; row<h/scale; row++) {
            for(int col=0; col<w/scale; col++) {

                int Y = 0;

                for(int s1=0; s1<scale; s1++) {
                    for(int s2=0; s2<scale; s2++) {
                        Y = (int) in[(row * scale + s1)*w + (col*scale) + s2] & 0xff;
                    }
                }
                out[row*w/scale + col] = (byte) Math.round(Y);
            }
        }

        int color_w = w/2;
        int color_h = h/2;

        for(int row=0; row<color_h/scale; row++) {
            for(int col=0; col<color_w/scale; col++) {

                int U = 0;
                int V = 0;

                for(int s1=0; s1<scale; s1++) {
                    for(int s2=0; s2<scale; s2++) {
                        V += (int) in[h * w + ((row * scale + s1) * w + col*scale*2) + (2*s2)] & 0xff;
                        U += (int) in[h * w + ((row * scale + s1) * w + col*scale*2) + (2*s2)+1] & 0xff;
                    }
                }
                out[(h*w/scale/scale) + (row*color_w/scale + col)*2] = (byte) Math.round(V/scale/scale);
                out[(h*w/scale/scale) + (row*color_w/scale + col)*2+1] = (byte) Math.round(U/scale/scale);
            }
        }


        // from now on, we are just padding the video to obtain a resolution that is
        // accepted by the encoder.
        byte[] padded = new byte[out.length/180*240];
        int nh = h/scale;
        int nw = w/scale;

        if (h>w) {

            for (int row = 0; row < nh; row++) {
                for (int col = 0; col < 30; col++)
                    padded[row * 240 + col] = (byte) 0 & 0xFF;
                for (int col = 210; col < 240; col++)
                    padded[row * 240 + col] = (byte) 0 & 0xFF;

                for (int col = 30; col < 210; col++)
                    padded[row * 240 + col] = out[row * nw + col - 30];
            }

            for (int row = 0; row < nh / 2; row++) {
                for (int col = 0; col < 15; col++) {
                    padded[nh * (nw + 60) + (row * 120 + col) * 2] = (byte) (128 & 0xFF);
                    padded[nh * (nw + 60) + (row * 120 + col) * 2 + 1] = (byte) (128 & 0xFF);
                }
                for (int col = 105; col < 120; col++) {
                    padded[nh * (nw + 60) + (row * 120 + col) * 2] = (byte) (128 & 0xFF);
                    padded[nh * (nw + 60) + (row * 120 + col) * 2 + 1] = (byte) (128 & 0xFF);
                }
                for (int col = 15; col < 105; col++) {
                    padded[nh * (nw + 60) + (row * 120 + col) * 2] = out[nw * nh + (row * nw / 2 + (col - 15)) * 2];
                    padded[nh * (nw + 60) + (row * 120 + col) * 2 + 1] = out[nw * nh + (row * nw / 2 + (col - 15)) * 2 + 1];
                }
            }
            return padded;

        } else {

            for (int col = 0; col < nw; col++) {
                for (int row = 0; row < 30; row++)
                    padded[row * 320 + col] = (byte) 0 & 0xFF;
                for (int row = 210; row < 240; row++)
                    padded[row * 320 + col] = (byte) 0 & 0xFF;

                for (int row = 30; row < 210; row++)
                    padded[row * 320 + col] = out[(row-30) * nw + col];
            }

            for (int col = 0; col < nw / 2; col++) {
                for (int row = 0; row < 15; row++) {
                    padded[nw * (nh+60) + (row * 160 + col) * 2] = (byte) (128 & 0xFF);
                    padded[nw * (nh+60) + (row * 160 + col) * 2 + 1] = (byte) (128 & 0xFF);
                }
                for (int row = 105; row < 120; row++) {
                    padded[nw * (nh+60) + (row * 160 + col) * 2] = (byte) (128 & 0xFF);
                    padded[nw * (nh+60) + (row * 160 + col) * 2 + 1] = (byte) (128 & 0xFF);
                }
                for (int row = 15; row < 105; row++) {
                    padded[nw * (nh+60) + (row * 160 + col) * 2] = out[nh * nw + ((row-15) * nw / 2 + col) * 2];
                    padded[nw * (nh+60) + (row * 160 + col) * 2 + 1] = out[nh * nw + ((row-15) * nw / 2 + col) * 2 + 1];
                }
            }
            return padded;
        }
    }
}
