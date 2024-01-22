#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

const int radius = 10;
const int total = (radius + 1)*(radius + 1);
varying vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture;
uniform vec2 u_offsets;

void main()
{
    vec4 col = vec4(0);
    float num = 0;
    for (int i = -radius; i <= radius; i++)
    {
        for (int j = -radius; j <= radius; j++)
        {
            vec4 new = texture2D(u_texture, vec2(v_texCoords.x + (u_offsets.x * i), v_texCoords.y + (u_offsets.y * j)));
            col += new;
            num += new.a;
        }
    }

    gl_FragColor = vec4(col.rgb / num, texture2D(u_texture, v_texCoords).a);
}