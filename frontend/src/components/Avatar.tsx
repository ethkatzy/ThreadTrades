import Image from "next/image";

type AvatarProps = {
  src: string | null;
  name: string;
  size?: number;
  className?: string;
};

export function Avatar({ src, name, size = 40, className }: AvatarProps) {
  return (
    <div
      className={`relative shrink-0 overflow-hidden rounded-full border border-border bg-surface ${className ?? ""}`}
      style={{ width: size, height: size }}
    >
      {src ? (
        <Image src={src} alt={name} fill sizes={`${size}px`} className="object-cover" />
      ) : (
        <span
          className="flex h-full w-full items-center justify-center font-medium text-muted"
          style={{ fontSize: size * 0.4 }}
          aria-hidden="true"
        >
          {name.charAt(0).toUpperCase()}
        </span>
      )}
    </div>
  );
}
