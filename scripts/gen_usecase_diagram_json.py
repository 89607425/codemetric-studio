import json
import math
import pathlib
import re
import xml.etree.ElementTree as ET


def local(tag: str) -> str:
    return tag.split("}", 1)[-1] if "}" in tag else tag


def all_by_local(node, name: str):
    return [e for e in node.iter() if local(e.tag) == name]


def child_text(node, name: str) -> str:
    for c in node.iter():
        if local(c.tag) == name:
            return (c.text or "").strip()
    return ""


def get_ref_id(node, container: str) -> str:
    # container is a direct child like Object1/Object2
    direct = None
    for ch in list(node):
        if local(ch.tag) == container:
            direct = ch
            break
    if direct is None:
        return ""
    for x in direct.iter():
        if "Ref" in x.attrib:
            return x.attrib.get("Ref", "")
    return ""


def parse_fp_tags(comment_text: str):
    pattern = re.compile(r"([A-Za-z]+)\s*=\s*([A-Za-z0-9_.-]+)")
    tags = {k.upper(): v for k, v in pattern.findall(comment_text or "")}
    return {
        "type": str(tags.get("TYPE", "")).upper(),
        "store": str(tags.get("STORE", "")).upper(),
        "ftr": int(tags.get("FTR", 0)),
        "der": int(tags.get("DER", 0)),
        "det": int(tags.get("DET", 0)),
        "ret": int(tags.get("RET", 0)),
    }


def main():
    repo_root = pathlib.Path(r"e:\1大三下\软件质量保证\experiment\codemetric-studio")
    oom_path = repo_root / "object" / "网上药店用例图.oom"
    out_path = repo_root / "online-pharmacy-usecase-java" / "usecase-diagram.json"
    out_path.parent.mkdir(parents=True, exist_ok=True)

    root = ET.parse(oom_path).getroot()

    actor_nodes = [n for n in all_by_local(root, "Actor") if n.get("Id") and child_text(n, "Name")]
    usecase_nodes = [
        n for n in all_by_local(root, "UseCase") if n.get("Id") and child_text(n, "Name")
    ]
    assocs = [n for n in all_by_local(root, "UseCaseAssociation") if n.get("Id")]

    actors = [{"id": n.get("Id"), "name": child_text(n, "Name")} for n in actor_nodes]
    use_cases = []
    for n in usecase_nodes:
        comment = child_text(n, "Comment")
        use_cases.append(
            {
                "id": n.get("Id"),
                "name": child_text(n, "Name"),
                "comment": comment,
                "fpTags": parse_fp_tags(comment),
            }
        )

    actor_map = {a["id"]: a for a in actors}
    usecase_map = {u["id"]: u for u in use_cases}

    actor_degree = {a["id"]: 0 for a in actors}
    usecase_degree = {u["id"]: 0 for u in use_cases}

    for assoc in assocs:
        o1 = get_ref_id(assoc, "Object1")
        o2 = get_ref_id(assoc, "Object2")
        if o1 in actor_map and o2 in usecase_map:
            actor_degree[o1] += 1
            usecase_degree[o2] += 1
        elif o2 in actor_map and o1 in usecase_map:
            actor_degree[o2] += 1
            usecase_degree[o1] += 1

    actorSimple = actorAverage = actorComplex = 0
    for deg in actor_degree.values():
        if deg >= 4:
            actorComplex += 1
        elif deg >= 2:
            actorAverage += 1
        else:
            actorSimple += 1

    useCaseSimple = useCaseAverage = useCaseComplex = 0
    for u in use_cases:
        deg = usecase_degree.get(u["id"], 0)
        score = deg + math.ceil(len(u["name"]) / 8)
        if score >= 5:
            useCaseComplex += 1
        elif score >= 3:
            useCaseAverage += 1
        else:
            useCaseSimple += 1

    out = {
        "actors": actors,
        "useCases": use_cases,
        "associations": len(assocs),
        "actorSimple": actorSimple,
        "actorAverage": actorAverage,
        "actorComplex": actorComplex,
        "useCaseSimple": useCaseSimple,
        "useCaseAverage": useCaseAverage,
        "useCaseComplex": useCaseComplex,
    }

    out_path.write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")
    print("Generated:", out_path)
    print("actors:", len(actors), "useCases:", len(use_cases), "associations:", len(assocs))


if __name__ == "__main__":
    main()

