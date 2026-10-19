import os
import shutil
import zipfile
import subprocess

def find_java_files(dir_path):
    files = []
    for root, _, filenames in os.walk(dir_path):
        for f in filenames:
            if f.endswith(".java"):
                files.append(os.path.join(root, f))
    return files

def run_cmd(cmd):
    print(f"Running: {cmd[:100]}... ({len(cmd)} chars)")
    res = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    if res.returncode != 0:
        print("STDOUT:", res.stdout)
        print("STDERR:", res.stderr)
        raise RuntimeError(f"Command failed with code {res.returncode}")

def zip_dir(source_dir, target_zip):
    with zipfile.ZipFile(target_zip, 'w', zipfile.ZIP_DEFLATED) as zf:
        for root, _, files in os.walk(source_dir):
            for file in files:
                abs_path = os.path.join(root, file)
                rel_path = os.path.relpath(abs_path, source_dir)
                zf.write(abs_path, rel_path)

def build_war():
    print("--- 1. Compiling modules ---")
    os.makedirs("out/production/Engine", exist_ok=True)
    os.makedirs("out/production/Server", exist_ok=True)

    # 1. Compile Engine (contains engine.dto, engine.model, engine.service, etc.)
    engine_files = find_java_files("Engine/src")
    run_cmd(f'javac -cp "lib/*" -d out/production/Engine {" ".join(engine_files)}')

    # Copy Engine XML resources if any
    if os.path.exists("Engine/src/resources"):
        shutil.copytree("Engine/src/resources", "out/production/Engine/resources", dirs_exist_ok=True)

    # 2. Compile Server
    server_files = find_java_files("Server/src")
    run_cmd(f'javac -cp "lib/*;out/production/Engine" -d out/production/Server {" ".join(server_files)}')

    print("--- 2. Assembling exploded WAR ---")
    exploded_dir = "out/artifacts/Server_war_exploded"
    if os.path.exists(exploded_dir):
        shutil.rmtree(exploded_dir)

    web_inf = os.path.join(exploded_dir, "WEB-INF")
    classes_dir = os.path.join(web_inf, "classes")
    lib_dir = os.path.join(web_inf, "lib")
    os.makedirs(classes_dir, exist_ok=True)
    os.makedirs(lib_dir, exist_ok=True)

    # Copy web.xml
    shutil.copy2("Server/web/WEB-INF/web.xml", os.path.join(web_inf, "web.xml"))

    # Copy Server compiled classes
    shutil.copytree("out/production/Server", classes_dir, dirs_exist_ok=True)

    # Build Engine.jar into lib
    zip_dir("out/production/Engine", os.path.join(lib_dir, "Engine.jar"))

    # Copy runtime JARs (excluding servlet-api since Tomcat provides it)
    runtime_jars = [
        "gson.jar",
        "jakarta.xml.bind-api.jar",
        "jaxb-impl.jar",
        "jaxb-core.jar",
        "jakarta.activation-api.jar",
        "angus-activation.jar"
    ]
    for jar in runtime_jars:
        src_jar = os.path.join("lib", jar)
        if os.path.exists(src_jar):
            shutil.copy2(src_jar, os.path.join(lib_dir, jar))

    print("--- 3. Packaging WAR file ---")
    war_dir = "out/artifacts/Server_war"
    os.makedirs(war_dir, exist_ok=True)
    war_path = os.path.join(war_dir, "guess_market.war")
    zip_dir(exploded_dir, war_path)
    shutil.copy2(war_path, os.path.join(war_dir, "Server_war.war"))

    print(f"Successfully built WAR: {war_path}")

if __name__ == "__main__":
    build_war()
