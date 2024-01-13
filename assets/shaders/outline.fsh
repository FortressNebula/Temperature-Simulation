#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

const int steps = 10;
varying vec4 v_color;
varying vec2 v_texCoords;
varying vec2 v_position;

uniform sampler2D u_texture;
uniform vec2 u_offsets;

void main()
{
    vec4 col = v_color * texture2D(u_texture, v_texCoords);
    if (col.a > 0.5)
        gl_FragColor = col;
    else
    {
        col = vec4(0,0,0,0);
//        col =
//        texture2D(u_texture, vec2(v_texCoords.x + u_offsets.x, v_texCoords.y)) +
//        texture2D(u_texture, vec2(v_texCoords.x - u_offsets.x, v_texCoords.y)) +
//        texture2D(u_texture, vec2(v_texCoords.x, v_texCoords.y + u_offsets.y)) +
//        texture2D(u_texture, vec2(v_texCoords.x, v_texCoords.y - u_offsets.y));
//
//        gl_FragColor = vec4(col.r / 4, col.g / 4, col.b / 4, sign(col.a));

        for (int i = -steps; i <= steps; i++)
        {
            for (int j = -steps; j <= steps; j++)
            {
                if (i == 0 && j == 0) continue;
                vec4 new = texture2D(u_texture, vec2(v_texCoords.x + (u_offsets.x * i), v_texCoords.y + (u_offsets.y * j)));
                col += vec4(new.rgb, new.a / 4 / (abs(i) + abs(j)));
            }
        }

        gl_FragColor = vec4(col.rgb / steps, col.a * col.r / 2);;
    }
}