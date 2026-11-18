"""홈 카드에 넣을 알약 그림을 그린다: 캡슐 하나 + 둥근 알약 하나, 부드러운 그림자. 바탕은 투명."""
import numpy as np
from PIL import Image, ImageFilter
SS = 3                      # 크게 그린 뒤 줄여서 가장자리를 부드럽게
W = H = 640 * SS
L = np.array([-0.45, -0.62, 0.64]); L = L / np.linalg.norm(L)     # 빛: 왼쪽 위 앞에서
V = np.array([0.0, 0.0, 1.0]); Hh = (L + V) / np.linalg.norm(L + V)
yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)

def shade(normal, base, mask, gloss=60, spec=0.55, ambient=0.50):
    nl = np.clip((normal * L).sum(-1), 0, 1)
    nh = np.clip((normal * Hh).sum(-1), 0, 1)
    rim = (1 - normal[..., 2]) ** 2.2                      # 가장자리가 살짝 어두워지게
    col = base * (ambient + (1 - ambient) * nl)[..., None] * (1 - 0.28 * rim)[..., None]
    col = col + spec * (nh ** gloss)[..., None] * 255
    out = np.zeros((H, W, 4), np.float32)
    out[..., :3] = np.clip(col, 0, 255); out[..., 3] = mask * 255
    return out

def capsule(cx, cy, length, radius, angle, colA, colB):
    a = np.deg2rad(angle); ux, uy = np.cos(a), np.sin(a)
    dx, dy = xx - cx, yy - cy
    t = dx * ux + dy * uy                                   # 축 방향 위치
    half = length / 2 - radius
    tc = np.clip(t, -half, half)
    px, py = dx - tc * ux, dy - tc * uy                     # 축에서 픽셀까지
    r = np.sqrt(px ** 2 + py ** 2) / radius
    mask = np.clip((1 - r) * radius / 1.5, 0, 1)            # 가장자리 1.5px 부드럽게
    rr = np.clip(r, 0, 0.999)
    n = np.stack([px / radius, py / radius, np.sqrt(1 - rr ** 2)], -1)
    base = np.where((t < 0)[..., None], np.array(colA, np.float32), np.array(colB, np.float32))
    img = shade(n, base, mask)
    seam = np.exp(-(t / (radius * 0.035)) ** 2)             # 두 반쪽이 만나는 가는 금
    img[..., :3] *= (1 - 0.22 * seam)[..., None]
    return img

def tablet(cx, cy, radius, colour, angle):
    dx, dy = xx - cx, yy - cy
    r = np.sqrt(dx ** 2 + dy ** 2) / radius
    mask = np.clip((1 - r) * radius / 1.5, 0, 1)
    bevel = np.clip((r - 0.72) / 0.28, 0, 1)                # 가운데는 평평, 가장자리만 둥글게
    tilt = bevel ** 1.6 * 0.95
    rs = np.maximum(r, 1e-4)
    n = np.stack([dx / radius / rs * tilt, dy / radius / rs * tilt, np.sqrt(np.clip(1 - tilt ** 2, 0, 1))], -1)
    img = shade(n, np.array(colour, np.float32) * np.ones((H, W, 3), np.float32), mask, gloss=30, spec=0.25, ambient=0.62)
    a = np.deg2rad(angle); d = -dx * np.sin(a) + dy * np.cos(a); along = dx * np.cos(a) + dy * np.sin(a)
    groove = np.exp(-(d / (radius * 0.045)) ** 2) * (np.abs(along) < radius * 0.74)
    img[..., :3] *= (1 - 0.20 * groove)[..., None]
    return img

def shadow(img, dx, dy, blur, alpha):
    a = Image.fromarray(img[..., 3].astype(np.uint8)).filter(ImageFilter.GaussianBlur(blur))
    a = np.roll(np.roll(np.asarray(a, np.float32), dy, 0), dx, 1) * alpha
    s = np.zeros((H, W, 4), np.float32); s[..., :3] = (6, 46, 54); s[..., 3] = a
    return s

def over(dst, src):
    sa = src[..., 3:4] / 255; da = dst[..., 3:4] / 255
    oa = sa + da * (1 - sa)
    rgb = (src[..., :3] * sa + dst[..., :3] * da * (1 - sa)) / np.maximum(oa, 1e-6)
    return np.concatenate([rgb, oa * 255], -1)

def scene(objs, name, out, shadows=True, s1=(16,22,20,0.42), s2=(4,6,5,0.30)):
    u = SS
    canvas = np.zeros((H, W, 4), np.float32)
    if shadows:
        for obj in objs: canvas = over(canvas, shadow(obj, s1[0]*u, s1[1]*u, s1[2]*u, s1[3]))
    for obj in objs:
        if shadows: canvas = over(canvas, shadow(obj, s2[0]*u, s2[1]*u, s2[2]*u, s2[3]))
        canvas = over(canvas, obj)
    img = Image.fromarray(np.clip(canvas, 0, 255).astype(np.uint8)).resize((out, out), Image.LANCZOS)
    img.save(name, optimize=True); return img

u = SS
WHITE, MINT, TEAL = (250, 252, 252), (120, 214, 200), (20, 150, 158)
# 1) 홈 카드: 캡슐 + 둥근 알약
scene([capsule(300*u, 268*u, 330*u, 62*u, -32, WHITE, MINT), tablet(452*u, 448*u, 72*u, (248, 250, 250), -32)], 'art/home_pills.png', 640)
# 2) 앱 아이콘의 그림: 가운데에 캡슐 하나 (흰색 + 민트), 그림자 포함. 가운데 61%(66/108) 안에 들어오게
icon = scene([capsule(320*u, 320*u, 300*u, 66*u, -40, WHITE, MINT)], 'art/ic_launcher_foreground_img.png', 432, s1=(10,16,16,0.40), s2=(3,5,5,0.28))
# 3) 머리말 로고: 캡슐 하나 (청록 + 민트). 흰 바탕 위에 놓이므로 그림자는 옅게
scene([capsule(320*u, 320*u, 500*u, 112*u, -40, TEAL, MINT)], 'art/logo_capsule.png', 192, s1=(6,12,14,0.16), s2=(2,4,5,0.14))
# 미리보기
from PIL import ImageDraw
pv = Image.new('RGBA', (432*2+40, 432), (255, 255, 255, 255))
bg = Image.new('RGBA', (432, 432), (18, 139, 148, 255)); bg.alpha_composite(icon); pv.paste(bg, (0, 0))
lg = Image.open('art/logo_capsule.png'); pv.alpha_composite(lg, (432+60, 120))
pv.convert('RGB').save('art/preview2.jpg', quality=90)
